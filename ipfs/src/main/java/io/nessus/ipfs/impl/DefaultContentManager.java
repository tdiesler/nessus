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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

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

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Config;
import io.nessus.Network;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.cipher.AESCipher;
import io.nessus.cipher.ECIESCipher;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.IPFSClient;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;

public class DefaultContentManager implements ContentManager {

    public static final String PREFIX = "DAT";
    public static final String VERSION = "1.0";
    public static final String VERSION_STRING = PREFIX + "-Version: " + VERSION;
    public static final String FILE_HEADER_END = PREFIX + "_HEADER_END";
    
    static final Logger LOG = LoggerFactory.getLogger(DefaultContentManager.class);

    final Blockchain blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL);
    final Network network = blockchain.getNetwork();
    final Wallet wallet = blockchain.getWallet();
    
    final IPFSClient ipfs;
    final BCData bcdata;
    final Path rootPath;
    
    // Contains the EC pubKey used to encrypt the AES SecretKey 
    // which results in the Token stored in the IPFS file header
    final Map<Address, PublicKey> keycache = new LinkedHashMap<>();
    
    // Contains fully initialized FHandles pointing to encrypted files.
    // It is guarantied that the wallet contains the privKeys needed to access these files.
    //
    // Storing a renduandant copy of content that is primarily managed in IPFS
    // is somewhat problematic. Because the here stored encrypted files are not subject 
    // to IPFS eviction policy, the lists may diverge possibly resulting in breakage of "show" on the gateway.
    // However, no all all the information needed by this app (e.g. path) can be stored on the blockchain.
    // Hence, an IPFS get is needed in any case to get at the IPFS file header. For large files this may result
    // in an undesired performance hit. We may need to find ways to separate this metadata from the actual content.
    final Map<String, FHandle> filecache = new LinkedHashMap<>();
    
    public DefaultContentManager() throws IOException {
        
        rootPath = Paths.get(System.getProperty("user.home"), ".fman");
        rootPath.toFile().mkdirs();
        
        bcdata = new BCData(getDataPrefix());
        ipfs = new CmdLineIPFSClient();
        
        Config config = Config.parseConfig("/initial-import.json");
        if (config != null) wallet.importAddresses(config);

        BigDecimal balance = wallet.getBalance("");
        if (balance.doubleValue() == 0.0) {
            Network network = blockchain.getNetwork();
            List<String> blocks = network.generate(101, null);
            AssertState.assertEquals(101, blocks.size());
        }
    }

    public Path getRootPath() {
        return rootPath;
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
        AssertState.assertNotNull(addr.getPrivKey(), "Wallet does not controll private key for: " + addr);

        // Do nothing if already registered
        PublicKey pubKey = findRegistation(addr);
        if (pubKey != null) return pubKey;

        // Store the EC key, which is derived from the privKey

        KeyPair keyPair = getECKeyPair(addr);
        pubKey = keyPair.getPublic();
        
        byte[] keyBytes = pubKey.getEncoded();
        byte[] data = bcdata.createPubKeyData(keyBytes);

        // Send a Tx to record the OP_RETURN data

        BigDecimal dataFee = getMinimumDataFee();
        List<UTXO> utxos = wallet.selectUnspent(Arrays.asList(addr), addFee(dataFee));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        BigDecimal changeAmount = utxosAmount.subtract(addFee(dataFee));

        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(new TxOutput(addr.getAddress(), changeAmount, data))
                .build();

        String txId = wallet.sendTx(tx);

        LOG.info("Register pubKey: {} => Tx {}", addr, txId);

        return pubKey;
    }

    @Override
    public FHandle add(Address owner, InputStream input, Path path) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(input, "input");
        
        PublicKey pubKey = findRegistation(owner);
        AssertArgument.assertNotNull(pubKey, "Cannot obtain encryption key for: " + owner);
        
        Path plainPath = assertPlainPath(owner, path);
        
        LOG.info("Adding: {} {}", owner, path);
        
        plainPath.getParent().toFile().mkdirs();
        Files.copy(input, plainPath, StandardCopyOption.REPLACE_EXISTING);
        
        URL furl = plainPath.toFile().toURI().toURL();
        FHandle fhandle = new FHBuilder(furl).owner(owner).path(path).build();
        
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
        
        PublicKey pubKey = findRegistation(target);
        AssertArgument.assertNotNull(pubKey, "Cannot obtain encryption key for: " + target);
        
        LOG.info("Sending: {} {}", owner, cid);
        
        FHandle fhandle = ipfsGet(cid, timeout, TimeUnit.MILLISECONDS);
        
        LOG.info("Decrypt: {}", fhandle);

        fhandle = decrypt(fhandle, owner, null);
        
        fhandle = new FHBuilder(fhandle).owner(target).cid(null).build();
        
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
        
        PublicKey pubKey;
        
        synchronized (keycache) {
            pubKey = keycache.get(addr);
            if (pubKey == null) {
                
                List<UTXO> utoxs = wallet.listUnspent(Arrays.asList(addr));
                for (int i = 0; pubKey == null && i < utoxs.size(); i++) {
                    
                    UTXO utox = utoxs.get(i);
                    String txId = utox.getTxId();
                    Tx tx = wallet.getTransaction(txId);
                    pubKey = getPubKeyFromTx(addr, tx);
                }
                
                if (pubKey != null) {
                    keycache.put(addr, pubKey);
                }
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
            
            for (UTXO utox : wallet.listUnspent(Arrays.asList(addr))) {
                
                String txId = utox.getTxId();
                Tx tx = wallet.getTransaction(txId);
                FHandle fhandle = getFHandleFromTx(addr, tx);
                String cid = fhandle != null ? fhandle.getCid() : null;
                if (cid != null && filecache.get(cid) == null) {
                    
                    fhandle = ipfsGet(cid, timeout, TimeUnit.MILLISECONDS);
                    
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
        if (!plainPath.toFile().isFile()) return null;
        
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

    protected BigDecimal getMinimumDataFee() {
        return BigDecimal.ZERO;
    }

    protected String getDataPrefix() {
        return PREFIX;
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
                
                URL furl = tmpPath.toUri().toURL();
                fhandle = new FHBuilder(furl).cid(cid).build();
                
                try (FileReader fr = new FileReader(tmpPath.toFile())) {
                    BufferedReader br = new BufferedReader(fr);

                    FHeader header = FHeader.read(br);
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
                
                fhandle =  new FHBuilder(fhandle)
                        .url(cryptPath.toUri().toURL())
                        .build();
            }
        }
        
        LOG.info("Found: {}", fhandle);
        
        return fhandle;
    }

    private FHandle recordFileData(FHandle fhandle, Address fromAddr, Address toAddr) throws GeneralSecurityException {
        AssertArgument.assertTrue(fhandle.isEncrypted(), "File not encrypted: " + fhandle);

        // Construct the OP_RETURN data

        byte[] data = bcdata.createFileData(fhandle);

        // Send a Tx to record the OP_TOKEN data

        Tx tx;
        BigDecimal dataFee = getMinimumDataFee();
        
        if (fromAddr.equals(toAddr)) {
            
            List<UTXO> utxos = wallet.selectUnspent(Arrays.asList(fromAddr), addFee(dataFee));
            BigDecimal changeAmount = getUTXOAmount(utxos).subtract(addFee(dataFee));
            tx = new TxBuilder()
                    .unspentInputs(utxos)
                    .output(new TxOutput(fromAddr.getAddress(), changeAmount, data))
                    .build();
        } else {
            
            BigDecimal doubleDataFee = dataFee.multiply(new BigDecimal(2));
            List<UTXO> utxos = wallet.selectUnspent(Arrays.asList(fromAddr), addFee(doubleDataFee));
            BigDecimal changeAmount = getUTXOAmount(utxos).subtract(addFee(doubleDataFee));
            tx = new TxBuilder()
                    .unspentInputs(utxos)
                    .output(new TxOutput(fromAddr.getAddress(), changeAmount))
                    .output(new TxOutput(toAddr.getAddress(), dataFee, data))
                    .build();
        }
        
        String txId = wallet.sendTx(tx);

        fhandle = new FHBuilder(fhandle).txId(txId).build();
        
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
        AssertState.assertNotNull(addr.getPrivKey(), "Wallet does not controll private key for: " + addr);

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

        Path cryptPath = getCryptPath(owner).resolve(fhandle.getCid());
        AssertState.assertTrue(cryptPath.toFile().exists(), "Cannot obtain file: " + cryptPath);

        destPath = destPath != null ? destPath : fhandle.getPath();
        AssertArgument.assertTrue(!destPath.isAbsolute(), "Given path must be relative: " + destPath);
        
        // Read the file header
        
        FHeader header;
        try (FileReader fr = new FileReader(cryptPath.toFile())) {
            header = FHeader.read(fr);
            AssertState.assertEquals(VERSION_STRING, header.version);
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
            String fid = new String(hashBytes);

            // Get the file from IPFS
            LOG.debug("File Tx: {} => {}", tx.txId(), fid);
            
            fhandle = new FHBuilder(fid)
                    .txId(tx.txId())
                    .owner(addr)
                    .build();
        }

        // The FHandle is not fully initialized
        // There has been no blocking IPFS access
        
        return fhandle;
    }
    
    private boolean isOurs(Tx tx) {

        // Expect two outputs
        List<TxOutput> outs = tx.outputs();
        if (outs.size() < 2) return false;

        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect an address
        Address addr = wallet.findAddress(out0.getAddress());
        if (addr == null) return false;

        // Expect data on the second output
        if (out1.getData() == null) return false;

        // Expect data to be our's
        byte[] txdata = out1.getData();
        return bcdata.isOurs(txdata);
    }

    private FHeader writeHeader(FHandle fhandle, Writer pw) throws GeneralSecurityException, IOException {
        
        String owner = fhandle.getOwner().getAddress();
        String base64Token = fhandle.getSecretToken();
        
        LOG.info("Token: {}", base64Token);
        
        FHeader header = new FHeader(VERSION_STRING, fhandle.getPath(), owner, base64Token, -1);
        header.write(pw);
        
        return header;
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

        static FHeader read(Reader rd) throws IOException {
            BufferedReader br = new BufferedReader(rd);
            
            // First line is the version
            String line = br.readLine();
            AssertState.assertTrue(line.startsWith(VERSION_STRING), "Invalid version: " + line);
            
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
                    } else if (line.startsWith(FILE_HEADER_END)) {
                        line = null;
                    }
                }
            }
            
            return new FHeader(version, path, owner, token, length);
        }

        void write(Writer wr) {
            PrintWriter pw = new PrintWriter(wr);
            
            // First line is the version
            pw.println(version);
            
            // Second is the location
            pw.println(String.format("Path: %s", path));
            
            // Then comes the owner
            pw.println(String.format("Owner: %s", owner));
            
            // Then comes the encryption token in Base64
            pw.println(String.format("Token: %s", token));
            
            // Then comes an end of header marker
            pw.println(FILE_HEADER_END);
        }
    }
    
    static class BCData {
        
        static final byte OP_PUB_KEY = 0x10;
        static final byte OP_FILE_DATA = 0x20;
        
        // OP_RETURN defined in Bitcoin
        static final byte BTC_RETURN = 0x6A; 
        
        final String OP_PREFIX;
        
        BCData(String prefix) {
            OP_PREFIX = prefix;
        }

        byte [] createPubKeyData(byte[] pubKey) {
            return buffer(OP_PUB_KEY, pubKey.length + 1)
                    .put((byte) pubKey.length) 
                    .put(pubKey)
                    .array();
        }

        byte [] extractPubKeyData(byte[] txdata) {
            if (extractOpCode(txdata) != OP_PUB_KEY) return null;
            byte[] data = extractData(txdata);
            int len = data[0];
            data = Arrays.copyOfRange(data, 1, 1 + len);
            return data;
        }
        
        byte [] createFileData(FHandle fhandle) {
            byte[] fid = fhandle.getCid().getBytes();
            return buffer(OP_FILE_DATA, fid.length + 1)
                    .put((byte) fid.length) 
                    .put(fid)
                    .array();
        }

        byte [] extractFileData(byte[] txdata) {
            if (extractOpCode(txdata) != OP_FILE_DATA) return null;
            byte[] data = extractData(txdata);
            int len = data[0];
            data = Arrays.copyOfRange(data, 1, 1 + len);
            return data;
        }

        boolean isOurs(byte[] txdata) {
            byte[] prefix = OP_PREFIX.getBytes();
            if (txdata[0] != BTC_RETURN) return false;
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
