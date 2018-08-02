package io.nessus.ipfs.impl;

/*-
 * #%L
 * Nessus :: IPFS
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.RpcClientSupport;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.cipher.AESCipher;
import io.nessus.cipher.ECIESCipher;
import io.nessus.cmd.TimeoutException;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.MerkleNotFoundException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class DefaultContentManager implements ContentManager {

    public static class HValues {
        
        public final String PREFIX;
        public final String VERSION;
        public final String VERSION_STRING;
        public final String FILE_HEADER_END;
        
        public HValues(String prefix, String version) {
            this.PREFIX = prefix;
            this.VERSION = version;
            this.VERSION_STRING = PREFIX + "-Version: " + VERSION;
            this.FILE_HEADER_END = PREFIX + "_HEADER_END";
        }
    }
    
    static final Logger LOG = LoggerFactory.getLogger(DefaultContentManager.class);

    protected final Blockchain blockchain;
    protected final Network network;
    protected final Wallet wallet;

    protected final HValues hvals;
    protected final IPFSClient ipfs;
    protected final BCData bcdata;
    protected final Path rootPath;

    // Contains fully initialized FHandles pointing to encrypted files.
    // It is guarantied that the wallet contains the privKeys needed to access these files.
    //
    // Storing a renduandant copy of content that is primarily managed in IPFS
    // is somewhat problematic. Because the here stored encrypted files are not subject 
    // to IPFS eviction policy, the lists may diverge possibly resulting in breakage of "show" on the gateway.
    // However, no all all the information needed by this app (e.g. path) can be stored on the blockchain.
    // Hence, an IPFS get is needed in any case to get at the IPFS file header. For large files this may result
    // in an undesired performance hit. We may need to find ways to separate this metadata from the actual content.
    private final Map<String, FHandle> filecache = new LinkedHashMap<>();
    
    private Integer minBlockHeight;

    public DefaultContentManager(IPFSClient ipfs, Blockchain blockchain) {
        this.blockchain = blockchain;
        this.ipfs = ipfs;
        
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        rootPath = Paths.get(System.getProperty("user.home"), ".fman");
        rootPath.toFile().mkdirs();
        
        hvals = getHeaderValues();
        bcdata = new BCData(hvals);
    }

    public Path getRootPath() {
        return rootPath;
    }
    
    public void setMinBlockHeight(Integer blockHeight) {
        LOG.info("Min block height: {}", blockHeight);
        this.minBlockHeight = blockHeight;
    }

    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }
    
    @Override
    public IPFSClient getIPFSClient() {
        return ipfs;
    }

    @Override
    public PublicKey register(Address addr) throws GeneralSecurityException {
        
        assertArgumentHasLabel(addr);
        assertArgumentHasPrivateKey(addr);
        assertArgumentNoChangeAddress(addr);

        // Do nothing if already registered
        PublicKey pubKey = findRegistation(addr);
        if (pubKey != null)
            return pubKey;

        // Store the EC key, which is derived from the privKey

        KeyPair keyPair = getECKeyPair(addr);
        pubKey = keyPair.getPublic();
        
        byte[] keyBytes = pubKey.getEncoded();
        byte[] data = bcdata.createPubKeyData(keyBytes);

        // Send a Tx to record the OP_RETURN data

        BigDecimal dataAmount = getRecordedDataAmount();
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = addr.getLabels().get(0);
        List<UTXO> utxos = wallet.selectUnspent(label, addFee(spendAmount));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(addFee(spendAmount));

        List<TxOutput> outputs = new ArrayList<>();
        if (network.getDustThreshold().compareTo(changeAmount) < 0) {
            outputs.add(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        outputs.add(new TxOutput(addr.getAddress(), dataAmount, data));
            
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .outputs(outputs)
                .build();

        String txId = wallet.sendTx(tx);

        LOG.info("Register pubKey: {} => Tx {}", addr, txId);

        tx = wallet.getTransaction(txId);
        int vout = tx.outputs().size() - 2;
        TxOutput dataOut = tx.outputs().get(vout);
        AssertState.assertEquals(addr.getAddress(), dataOut.getAddress());
        AssertState.assertEquals(dataAmount, dataOut.getAmount());

        LOG.info("Lock unspent: {} {}", txId, vout);
        
        BitcoindRpcClient client = getRpcClient();
        client.lockUnspent(false, txId, vout);
        
        LOG.info("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, addr);
        
        return pubKey;
    }

    @Override
    public FHandle add(Address owner, InputStream input, Path path) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(input, "Null input");
        
        assertArgumentHasPrivateKey(owner);
        
        PublicKey pubKey = findRegistation(owner);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + owner);
        
        Path plainPath = assertPlainPath(owner, path);
        
        LOG.info("Adding: {} {}", owner, path);
        
        plainPath.getParent().toFile().mkdirs();
        Files.copy(input, plainPath, StandardCopyOption.REPLACE_EXISTING);
        
        URL furl = plainPath.toFile().toURI().toURL();
        FHandle fhandle = new FHBuilder(furl)
                .owner(owner)
                .path(path)
                .build();

        LOG.info("Encrypt: {}", fhandle);
        
        fhandle = encrypt(fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle);
        
        Path auxPath = Paths.get(fhandle.getURL().getPath());
        String cid = ipfs.add(auxPath, true);
        
        // Move the temp file to its crypt path
        
        Path cryptPath = getCryptPath(owner).resolve(cid);
        Path tmpPath = Paths.get(fhandle.getURL().getPath());
        Files.move(tmpPath, cryptPath, StandardCopyOption.REPLACE_EXISTING);
        
        furl = cryptPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("Record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, owner, owner);
        
        LOG.info("Done: {}", fhandle);
        
        return fhandle;
    }

    @Override
    public FHandle get(Address owner, String cid, Path path, Long timeout) throws IOException, GeneralSecurityException {

        assertArgumentHasPrivateKey(owner);

        Path plainPath = assertPlainPath(owner, path);
        
        LOG.info("Getting: {} {}", owner, path);
        
        FHandle fhandle = ipfsGet(cid, timeout, TimeUnit.MILLISECONDS);
        
        LOG.info("Decrypt: {}", fhandle);
        
        fhandle = decrypt(fhandle, owner, path);
        
        plainPath.getParent().toFile().mkdirs();
        
        Path tmpPath = Paths.get(fhandle.getURL().getPath());
        Files.move(tmpPath, plainPath, StandardCopyOption.REPLACE_EXISTING);
        
        URL furl = plainPath.toFile().toURI().toURL();
        fhandle = new FHBuilder(furl)
                .owner(owner)
                .path(path)
                .build();
        
        LOG.info("Done: {}", fhandle);
        
        return fhandle;
    }
    
    @Override
    public FHandle send(Address owner, String cid, Address target, Long timeout) throws IOException, GeneralSecurityException {
        
        assertArgumentHasLabel(owner);
        assertArgumentHasPrivateKey(owner);
        assertArgumentNoChangeAddress(owner);
        
        PublicKey pubKey = findRegistation(target);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + target);
        
        LOG.info("Sending: {} {}", owner, cid);
        
        FHandle fhandle = ipfsGet(cid, timeout, TimeUnit.MILLISECONDS);
        
        LOG.info("Decrypt: {}", fhandle);

        fhandle = decrypt(fhandle, owner, null);

        fhandle = new FHBuilder(fhandle)
                .owner(target)
                .cid(null)
                .build();

        LOG.info("Encrypt: {}", fhandle);
        
        fhandle = encrypt(fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle);
        
        Path tmpPath = Paths.get(fhandle.getURL().getPath());
        cid = ipfs.add(tmpPath, true);
        
        Path cryptPath = getCryptPath(target).resolve(cid);
        Files.move(tmpPath, cryptPath, StandardCopyOption.REPLACE_EXISTING);

        URL furl = cryptPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("Record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, owner, target);
        
        LOG.info("Done: {}", fhandle);
        
        return fhandle;
    }

    @Override
    public PublicKey findRegistation(Address addr) {
        
        // We used to have a cache of these pubKey registrations.
        // This is no longer the case, because we want the owner 
        // to have full control over the registration. If the 
        // registration UTXO gets spent, it will immediately
        // no longer available through this method.  
        
        PublicKey pubKey = null;
        
        List<UTXO> locked = listLockedAndUnlockedUnspent(addr, true, false);
        
        for (UTXO utxo : listLockedAndUnlockedUnspent(addr, true, true)) {
            
            String txId = utxo.getTxId();
            Tx tx = wallet.getTransaction(txId);
            
            pubKey = getPubKeyFromTx(addr, tx);
            if (pubKey != null) {
                
                // The lock state of a registration may get lost due to wallet 
                // restart. Here we recreate that lock state if the given
                // address owns the registration
                
                if (!locked.contains(utxo) && addr.getPrivKey() != null) {
                    
                    int vout = tx.outputs().size() - 2;
                    TxOutput dataOut = tx.outputs().get(vout);
                    AssertState.assertEquals(addr.getAddress(), dataOut.getAddress());
                    AssertState.assertEquals(getRecordedDataAmount(), dataOut.getAmount());
                    
                    LOG.info("Lock unspent: {} {}", txId, vout);
                    
                    wallet.lockUnspent(utxo, false);
                }
                
                break;
            }
        }
        
        return pubKey;
    }

    @Override
    public List<FHandle> findIPFSContent(Address addr, Long timeout) throws IOException {

        Map<String, FHandle> result = new LinkedHashMap<>();
        
        synchronized (filecache) {
        
            for (FHandle fhandle : filecache.values()) {
                if (fhandle.getOwner().equals(addr)) {
                    result.put(fhandle.getCid(), fhandle);
                }
            }
            
            List<UTXO> locked = listLockedAndUnlockedUnspent(addr, true, false);
            
            for (UTXO utxo : listLockedAndUnlockedUnspent(addr, true, true)) {
                
                String txId = utxo.getTxId();
                Tx tx = wallet.getTransaction(txId);
                
                FHandle fhandle = getFHandleFromTx(addr, tx);
                if (fhandle == null) continue;
                
                String cid = fhandle.getCid();
                if (result.containsKey(cid)) continue;
                
                if (hasMinBlockHeight(tx, 0)) {
                    
                    try {
                        fhandle = ipfsGet(cid, timeout, TimeUnit.MILLISECONDS);
                    } catch (IPFSException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof MerkleNotFoundException || cause instanceof TimeoutException) {
                            continue;
                        } else {
                            throw ex;
                        }
                    }
                    
                    if (!locked.contains(utxo)) {
                        
                        int vout = tx.outputs().size() - 2;
                        TxOutput dataOut = tx.outputs().get(vout);
                        AssertState.assertEquals(addr.getAddress(), dataOut.getAddress());
                        AssertState.assertEquals(getRecordedDataAmount(), dataOut.getAmount());
                        
                        LOG.info("Lock unspent: {} {}", txId, vout);
                        
                        BitcoindRpcClient client = getRpcClient();
                        client.lockUnspent(false, txId, vout);
                    }
                    
                    fhandle = new FHBuilder(fhandle)
                            .txId(txId)
                            .build();
                    
                    filecache.put(cid, fhandle);
                    result.put(cid, fhandle);
                }
            }
        }
        
        return new ArrayList<>(result.values());
    }

    @Override
    public List<FHandle> findLocalContent(Address owner) throws IOException {
        
        return findLocalContent(owner, getPlainPath(owner), new ArrayList<>());
    }

    private List<FHandle> findLocalContent(Address owner, Path fullPath, List<FHandle> fhandles) throws IOException {
        
        if (fullPath.toFile().isDirectory()) {
            for (String child : fullPath.toFile().list()) {
                findLocalContent(owner, fullPath.resolve(child), fhandles);
            }
        }
        
        if (fullPath.toFile().isFile()) {
            Path relPath = getPlainPath(owner).relativize(fullPath);
            URL furl = fullPath.toUri().toURL();
            
            FHandle fhandle = new FHBuilder(furl)
                    .path(relPath)
                    .owner(owner)
                    .build();
            
            fhandles.add(fhandle);
        }
        
        return fhandles;
    }
    
    @Override
    public InputStream getLocalContent(Address owner, Path path) throws IOException {
        
        Path plainPath = assertPlainPath(owner, path);
        if (!plainPath.toFile().isFile())
            return null;

        return new FileInputStream(plainPath.toFile());
    }

    @Override
    public boolean deleteLocalContent(Address owner, Path path) throws IOException {
        
        Path plainPath = assertPlainPath(owner, path);
        if (plainPath.toFile().isDirectory()) {
            for (String child : plainPath.toFile().list()) {
                deleteLocalContent(owner, path.resolve(child));
            }
        }
        
        if (plainPath.toFile().isFile()) {
            return plainPath.toFile().delete();
        }
        
        return false;
    }

    protected BitcoindRpcClient getRpcClient() {
        return ((RpcClientSupport) blockchain).getRpcClient();
    }

    protected HValues getHeaderValues() {
        return new HValues("DAT", "1.0");
    }

    Path getPlainPath(Address owner) {
        Path path = rootPath.resolve("plain").resolve(owner.getAddress());
        path.toFile().mkdirs();
        return path;
    }

    Path getCryptPath(Address owner) {
        Path path = rootPath.resolve("crypt").resolve(owner.getAddress());
        path.toFile().mkdirs();
        return path;
    }

    Path getTempPath() {
        Path tmpPath = rootPath.resolve("tmp");
        tmpPath.toFile().mkdirs();
        return tmpPath;
    }

    Path createTempFile() throws IOException {
        return Files.createTempFile(getTempPath(), "", "");
    }
    
    FHandle ipfsGet(String cid, Long timeout, TimeUnit unit) throws IOException {
        
        FHandle fhandle;
        synchronized (filecache) {
            
            fhandle = filecache.get(cid);
            
            if (fhandle == null) {
                
                Path tmpPath = getTempPath().resolve(cid);
                ipfs.get(cid, tmpPath.getParent(), timeout, unit);
                
                AssertState.assertTrue(tmpPath.toFile().exists(), "Cannot obtain file from: " + tmpPath);

                fhandle = new FHBuilder(tmpPath.toUri().toURL())
                        .cid(cid)
                        .build();

                try (FileReader fr = new FileReader(tmpPath.toFile())) {
                    BufferedReader br = new BufferedReader(fr);

                    FHeader header = FHeader.read(hvals, br);
                    Address addr = getAddress(header.owner);
                    AssertState.assertNotNull(addr, "Address unknown to this wallet: " + header.owner);

                    LOG.debug("Token: {} => {}", cid, header.token);

                    fhandle = new FHBuilder(fhandle)
                            .owner(getAddress(header.owner))
                            .secretToken(header.token)
                            .path(header.path)
                            .build();
                }
                
                Path cryptPath = getCryptPath(fhandle.getOwner()).resolve(cid);
                Files.move(tmpPath, cryptPath, StandardCopyOption.REPLACE_EXISTING);

                fhandle = new FHBuilder(fhandle)
                        .url(cryptPath.toUri().toURL())
                        .build();
            }
        }
        
        LOG.info("Found: {}", fhandle);
        
        return fhandle;
    }

    private boolean hasMinBlockHeight(Tx tx, int offset) {
        boolean result = true;
        if (minBlockHeight != null && tx.blockHash() != null) {
            int height = network.getBlock(tx.blockHash()).height();
            result = (minBlockHeight - offset) <= height;
        }
        return result;
    }

    private FHandle recordFileData(FHandle fhandle, Address fromAddr, Address toAddr) throws GeneralSecurityException {

        AssertArgument.assertTrue(fhandle.isEncrypted(), "File not encrypted: " + fhandle);

        // Construct the OP_RETURN data

        byte[] data = bcdata.createFileData(fhandle);

        // Send a Tx to record the OP_TOKEN data

        BigDecimal dataAmount = getRecordedDataAmount();
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = fromAddr.getLabels().get(0);
        List<UTXO> utxos = wallet.selectUnspent(label, addFee(spendAmount));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(addFee(spendAmount));

        List<TxOutput> outputs = new ArrayList<>();
        if (network.getDustThreshold().compareTo(changeAmount) < 0) {
            outputs.add(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        outputs.add(new TxOutput(toAddr.getAddress(), dataAmount, data));
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .outputs(outputs)
                .build();

        String txId = wallet.sendTx(tx);
        
        // Lock the UTXO if we have the priv key for the recipient 
        
        if (toAddr.getPrivKey() != null) {
            
            tx = wallet.getTransaction(txId);
            int vout = tx.outputs().size() - 2;
            TxOutput dataOut = tx.outputs().get(vout);
            AssertState.assertEquals(toAddr.getAddress(), dataOut.getAddress());
            AssertState.assertEquals(dataAmount, dataOut.getAmount());

            LOG.info("Lock unspent: {} {}", txId, vout);
            
            BitcoindRpcClient client = getRpcClient();
            client.lockUnspent(false, txId, vout);
        }

        LOG.info("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, fromAddr);
        
        fhandle = new FHBuilder(fhandle)
                .txId(txId)
                .build();

        return fhandle;
    }

    private Address getAddress(String rawAddr) {
        Address addrs = wallet.findAddress(rawAddr);
        AssertState.assertNotNull(addrs, "Address not known to this wallet: " + rawAddr);
        return addrs;
    }

    private SecretKey getAESKey(byte[] token) {
        String encoded = Base64.getEncoder().encodeToString(token);
        return new AESCipher().decodeSecretKey(encoded);
    }

    private KeyPair getECKeyPair(Address addr) throws GeneralSecurityException {
        
        assertArgumentHasPrivateKey(addr);

        // Decode the priv key as base64 (even though it might be WIF encoded)
        byte[] rawKey = Base64.getDecoder().decode(addr.getPrivKey());

        // Reverse the bytes to ignore possible constant coin prefix 
        byte[] seed = org.bouncycastle.util.Arrays.reverse(rawKey);

        // Derive the corresponding deterministic EC key pair  
        ECIESCipher cipher = new ECIESCipher();
        KeyPair keyPair = cipher.generateKeyPair(seed);

        return keyPair;
    }

    private BigDecimal addFee(BigDecimal amount) {
        return amount.add(network.estimateFee());
    }

    private BigDecimal getUTXOAmount(List<UTXO> utxos) {
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    private FHandle encrypt(FHandle fhandle, PublicKey pubKey) throws IOException, GeneralSecurityException {
        AssertArgument.assertTrue(!fhandle.isEncrypted(), "File already encrypted: " + fhandle);
        
        // Get the AES secret key
        AESCipher acipher = new AESCipher();
        SecretKey secKey = acipher.getSecretKey();

        ECIESCipher cipher = new ECIESCipher();
        byte[] encKey = cipher.encrypt(pubKey, secKey.getEncoded());
        String secToken = Base64.getEncoder().encodeToString(encKey);
        
        // Create the target file handle
        fhandle = new FHBuilder(fhandle)
                .secretToken(secToken)
                .cid(null)
                .build();

        File tmpFile = createTempFile().toFile();
        try (FileWriter fw = new FileWriter(tmpFile)) {
            
            // Writhe the file header
            writeHeader(fhandle, fw);
            
            try (InputStream ins = fhandle.getURL().openStream()) {
                
                // Encrypt the file content
                InputStream encrypted = acipher.encrypt(secKey, ins);
                
                // Hex encode the encrypted content
                // [TODO] provide a stream based hex encoder
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StreamUtils.copyStream(encrypted, baos);
                String base64Encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
                
                // Append encrypted file content
                fw.write(base64Encoded);
            }
        }
        
        return new FHBuilder(fhandle)
                .url(tmpFile.toURI().toURL())
                .build();
    }
    
    private FHandle decrypt(FHandle fhandle, Address owner, Path destPath) throws IOException, GeneralSecurityException {

        Path cryptPath = getCryptPath(fhandle.getOwner()).resolve(fhandle.getCid());
        AssertState.assertTrue(cryptPath.toFile().exists(), "Cannot obtain file: " + cryptPath);

        destPath = destPath != null ? destPath : fhandle.getPath();
        AssertArgument.assertTrue(!destPath.isAbsolute(), "Given path must be relative: " + destPath);
        
        // Read the file header
        
        FHeader header;
        try (FileReader fr = new FileReader(cryptPath.toFile())) {
            header = FHeader.read(hvals, fr);
            AssertState.assertEquals(hvals.VERSION_STRING, header.version);
        }
        
        // Read the content
        try (FileReader fr = new FileReader(cryptPath.toFile())) {

            // Skip the header
            for (int i = 0; i < header.length; i++) {
                fr.read();
            }
            
            ECIESCipher cipher = new ECIESCipher();
            KeyPair keyPair = getECKeyPair(owner);
            PrivateKey privKey = keyPair.getPrivate();
            byte[] encToken = Base64.getDecoder().decode(header.token);
            byte[] token = cipher.decrypt(privKey, encToken);
            SecretKey secKey = getAESKey(token);

            // Read Base64 encoded content
            String base64Encoded = new BufferedReader(fr).readLine();
            byte[] encBytes = Base64.getDecoder().decode(base64Encoded);
            ByteArrayInputStream ins = new ByteArrayInputStream(encBytes);
            
            // Decrypt the file content
            InputStream decrypted = new AESCipher().decrypt(secKey, ins);
            
            Path tmpFile = createTempFile();
            Files.copy(decrypted, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            
            return new FHBuilder(fhandle)
                    .url(tmpFile.toUri().toURL())
                    .secretToken(null)
                    .path(destPath)
                    .build();
        }
    }
    
    private PublicKey getPubKeyFromTx(Address addr, Tx tx) {

        if (!isOurs(tx)) 
            return null;
        
        PublicKey pubKey = null;

        List<TxOutput> outs = tx.outputs();
        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect OP_PUB_KEY
        byte[] txdata = out1.getData();
        byte[] keyBytes = bcdata.extractPubKeyData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (keyBytes != null && outAddr.equals(addr)) {

            String encKey = Base64.getEncoder().encodeToString(keyBytes);
            
            LOG.info("PubKey Tx: {} => {} => {}", new Object[] { tx.txId(), outAddr, encKey });
            
            pubKey = new PublicKey() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getFormat() {
                    return "X.509";
                }

                @Override
                public byte[] getEncoded() {
                    return keyBytes;
                }

                @Override
                public String getAlgorithm() {
                    return "EC";
                }
            };
        }

        return pubKey;
    }

    private FHandle getFHandleFromTx(Address addr, Tx tx) {

        if (!isOurs(tx)) 
            return null;
        
        FHandle fhandle = null;

        List<TxOutput> outs = tx.outputs();
        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect OP_FILE_DATA
        byte[] txdata = out1.getData();
        byte[] hashBytes = bcdata.extractFileData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (hashBytes != null && outAddr.equals(addr)) {

            // Get the file id from stored data
            String cid = new String(hashBytes);

            // Get the file from IPFS
            LOG.debug("File Tx: {} => {}", tx.txId(), cid);
            
            fhandle = new FHBuilder(cid)
                    .txId(tx.txId())
                    .owner(addr)
                    .build();
        }

        // The FHandle is not fully initialized
        // There has been no blocking IPFS access
        
        return fhandle;
    }
    
    private List<UTXO> listLockedAndUnlockedUnspent(Address addr, boolean locked, boolean unlocked) {
        
        List<UTXO> result = new ArrayList<>();
        
        if (unlocked) {
            result.addAll(wallet.listUnspent(Arrays.asList(addr)));
        }
        
        if (locked) {
            result.addAll(wallet.listLockUnspent(Arrays.asList(addr)));
        }
        
        return result;
    }

    private boolean isOurs(Tx tx) {

        // Expect two outputs
        List<TxOutput> outs = tx.outputs();
        if (outs.size() < 2)
            return false;

        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect an address
        Address addr = wallet.findAddress(out0.getAddress());
        if (addr == null)
            return false;

        // Expect data on the second output
        if (out1.getData() == null)
            return false;

        // Expect data to be our's
        byte[] txdata = out1.getData();
        return bcdata.isOurs(txdata);
    }

    private FHeader writeHeader(FHandle fhandle, Writer pw) throws GeneralSecurityException, IOException {
        
        String owner = fhandle.getOwner().getAddress();
        String base64Token = fhandle.getSecretToken();
        
        LOG.info("Token: {}", base64Token);
        
        FHeader header = new FHeader(hvals.VERSION_STRING, fhandle.getPath(), owner, base64Token, -1);
        header.write(hvals, pw);
        
        return header;
    }
    
    // Use 10 * what the network considers as dust as locked data amount
    private BigDecimal getRecordedDataAmount() {
        return network.getDustThreshold().multiply(BigDecimal.TEN);
    }

    private void assertArgumentHasPrivateKey(Address addr) {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);
    }

    private void assertArgumentHasLabel(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().isEmpty(), "Address has no label: " + addr);
    }
    
    private void assertArgumentNoChangeAddress(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().contains(Wallet.LABEL_CHANGE), "Cannot use change address: " + addr);
    }
    
    private Path assertPlainPath(Address owner, Path path) {
        AssertArgument.assertTrue(path != null && !path.isAbsolute(), "Not a relative path: " + path);
        return getPlainPath(owner).resolve(path);
    }

    static class FHeader {
        
        final String version;
        final Path path;
        final String owner;
        final String token;
        final int length;
        
        FHeader(String version, Path path, String owner, String token, int length) {
            this.version = version;
            this.path = path;
            this.owner = owner;
            this.token = token;
            this.length = length;
        }

        static FHeader read(HValues hvals, Reader rd) throws IOException {
            BufferedReader br = new BufferedReader(rd);
            
            // First line is the version
            String line = br.readLine();
            AssertState.assertTrue(line.startsWith(hvals.VERSION_STRING), "Invalid version: " + line);
            
            String version = line;
            Path path = null;
            String owner = null;
            String token = null;
            
            int length = line.length() + 1;

            // Read more header lines
            while (line != null) {
                line = br.readLine();
                if (line != null) {
                    
                    length += line.length() + 1;
                    
                    if (line.startsWith("Path: ")) {
                        path = Paths.get(line.substring(6));
                    } else if (line.startsWith("Owner: ")) {
                        owner = line.substring(7);
                    } else if (line.startsWith("Token: ")) {
                        token = line.substring(7);
                    } else if (line.startsWith(hvals.FILE_HEADER_END)) {
                        line = null;
                    }
                }
            }
            
            return new FHeader(version, path, owner, token, length);
        }

        void write(HValues hvals, Writer wr) {
            PrintWriter pw = new PrintWriter(wr);
            
            // First line is the version
            pw.println(hvals.VERSION_STRING);
            
            // Second is the location
            pw.println(String.format("Path: %s", path));
            
            // Then comes the owner
            pw.println(String.format("Owner: %s", owner));
            
            // Then comes the encryption token in Base64
            pw.println(String.format("Token: %s", token));
            
            // Then comes an end of header marker
            pw.println(hvals.FILE_HEADER_END);
        }
    }
    
    static class BCData {
        
        static final byte OP_PUB_KEY = 0x10;
        static final byte OP_FILE_DATA = 0x20;
        static final byte OP_RETURN = 0x6A; 
        
        final String OP_PREFIX;
        
        BCData(HValues hvals) {
            OP_PREFIX = hvals.PREFIX;
        }

        byte[] createPubKeyData(byte[] pubKey) {
            return buffer(OP_PUB_KEY, pubKey.length + 1).put((byte) pubKey.length).put(pubKey).array();
        }

        byte[] extractPubKeyData(byte[] txdata) {
            if (extractOpCode(txdata) != OP_PUB_KEY)
                return null;
            byte[] data = extractData(txdata);
            int len = data[0];
            data = Arrays.copyOfRange(data, 1, 1 + len);
            return data;
        }

        byte[] createFileData(FHandle fhandle) {
            byte[] fid = fhandle.getCid().getBytes();
            return buffer(OP_FILE_DATA, fid.length + 1).put((byte) fid.length).put(fid).array();
        }

        byte[] extractFileData(byte[] txdata) {
            if (extractOpCode(txdata) != OP_FILE_DATA)
                return null;
            byte[] data = extractData(txdata);
            int len = data[0];
            data = Arrays.copyOfRange(data, 1, 1 + len);
            return data;
        }

        boolean isOurs(byte[] txdata) {
            byte[] prefix = OP_PREFIX.getBytes();
            if (txdata[0] != OP_RETURN) return false;
            if (txdata[1] != txdata.length - 2) return false;
            byte[] aux = Arrays.copyOfRange(txdata, 2, 2 + prefix.length);
            return Arrays.equals(prefix, aux);
        }

        byte extractOpCode(byte[] txdata) {
            if (!isOurs(txdata)) return -1;
            byte[] prefix = OP_PREFIX.getBytes();
            byte opcode = txdata[prefix.length + 2];
            return opcode;
        }

        private byte[] extractData(byte[] txdata) {
            if (!isOurs(txdata)) return null;
            byte[] prefix = OP_PREFIX.getBytes();
            return Arrays.copyOfRange(txdata, 2 + prefix.length + 1, txdata.length);
        }

        private ByteBuffer buffer(byte op, int dlength) {
            byte[] prefix = OP_PREFIX.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(prefix.length + 1 + dlength);
            buffer.put(prefix);
            buffer.put(op);
            return buffer;
        }
        
    }
}
