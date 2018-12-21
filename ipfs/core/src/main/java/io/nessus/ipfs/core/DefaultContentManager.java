package io.nessus.ipfs.core;

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
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.multihash.Multihash;
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
import io.nessus.cipher.RSACipher;
import io.nessus.cipher.utils.AESUtils;
import io.nessus.cipher.utils.RSAUtils;
import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.AHandle.AHBuilder;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.FHandle.FHWalker;
import io.nessus.ipfs.FHandle.FHWalker.Visitor;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.ipfs.NessusUserFault;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.FileUtils;
import io.nessus.utils.StreamUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class DefaultContentManager implements ContentManager {

    static final Logger LOG = LoggerFactory.getLogger(DefaultContentManager.class);

    protected final ContentManagerConfig config;
    protected final IPFSClient ipfsClient;
    protected final Blockchain blockchain;
    protected final Network network;
    protected final Wallet wallet;
    protected final FHeaderValues fhvals;

    protected final AHandleManager ahmgr;
    protected final FHandleManager fhmgr;
    
    // Contains fully initialized FHandles pointing to encrypted files.
    // It is guarantied that the wallet contains the privKeys needed to access these files.
    //
    // Storing a renduandant copy of content that is primarily managed in IPFS
    // is somewhat problematic. Because the here stored encrypted files are not subject 
    // to IPFS eviction policy, the lists may diverge possibly resulting in breakage of "show" on the gateway.
    // However, not all the information needed by this app (e.g. path) can be stored on the blockchain.
    // Hence, an IPFS get is needed in any case to get at the IPFS file header. For large files this may result
    // in an undesired performance hit. We may need to find ways to separate this metadata from the actual content.
    private final IPFSCache ipfsCache = new IPFSCache();
    
    public DefaultContentManager(ContentManagerConfig config) {
    	this.config = config;

        ipfsClient = config.getIPFSClient();
        blockchain = config.getBlockchain();
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        fhvals = getFHeaderValues();
        ahmgr = new AHandleManager(this);
        fhmgr = new FHandleManager(this);
        
        LOG.info("{}{}", getClass().getSimpleName(), config);
    }

    public DefaultContentManager(IPFSClient ipfsClient, Blockchain blockchain, ContentManagerConfig config) {
    	this.ipfsClient = ipfsClient;
    	this.blockchain = blockchain;
    	this.config = config;

        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        fhvals = getFHeaderValues();
        ahmgr = new AHandleManager(this);
        fhmgr = new FHandleManager(this);
    }

    public ContentManagerConfig getConfig() {
        return config;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }
    
    public IPFSClient getIPFSClient() {
        return ipfsClient;
    }

    public IPFSCache getIPFSCache() {
		return ipfsCache;
	}

	public AHandleManager getAHandleManager() {
		return ahmgr;
	}

	public FHandleManager getFHandleManager() {
		return fhmgr;
	}

	@Override
    public AHandle registerAddress(Address owner) throws GeneralSecurityException, IOException {
    	AHandle ahandle = registerAddress(owner, false);
		return ahandle;
    }
    
    public AHandle registerAddress(Address owner, boolean dryRun) throws GeneralSecurityException, IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        assertArgumentHasLabel(owner);
        assertArgumentHasPrivateKey(owner);
        assertArgumentNotChangeAddress(owner);

        // Do nothing if already registered
        
        AHandle ahandle = findAddressRegistation(owner, null);
        if (ahandle != null && ahandle.isAvailable())
            return ahandle;

        // Generate a new RSA key pair derived from the private blockchain key

        KeyPair keyPair = RSAUtils.newKeyPair(owner);
        PublicKey pubKey = keyPair.getPublic();
        
        ahandle = new AHBuilder(owner, pubKey).build();
        ahandle = ahmgr.addIpfsContent(ahandle, dryRun);
        Multihash cid = ahandle.getCid();
        
        byte[] data = ahmgr.createAddrData(cid);

        // Send a Tx to record the OP_RETURN data

        Network network = getBlockchain().getNetwork();
        Wallet wallet = getBlockchain().getWallet();
        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = ahandle.getLabel();
        List<UTXO> utxos = wallet.selectUnspent(label, spendAmount.add(feePerKB));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(spendAmount.add(feePerKB));

        List<TxOutput> outputs = new ArrayList<>();
        if (dustAmount.compareTo(changeAmount) < 0) {
            outputs.add(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        outputs.add(new TxOutput(owner.getAddress(), dataAmount, data));
            
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .outputs(outputs)
                .build();

        String txId = wallet.sendTx(tx);

        LOG.info("Register PubKey: {} => Tx {} => {}", owner.getAddress(), txId, cid);

        tx = wallet.getTransaction(txId);
        int vout = tx.outputs().size() - 2;
        TxOutput dataOut = tx.outputs().get(vout);
        AssertState.assertEquals(owner.getAddress(), dataOut.getAddress());
        AssertState.assertEquals(dataAmount, dataOut.getAmount());

        // Lock the UTXO
        
        List<UTXO> unlocked = ahmgr.listLockedAndUnlockedUnspent(owner, false, true);
        unlocked.stream()
        	.filter(utxo -> utxo.getTxId().equals(txId))
        	.filter(utxo -> utxo.getVout() == vout)
        	.forEach(utxo -> wallet.lockUnspent(utxo, false));
        
        LOG.debug("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, owner);
        
        AHandle ahres = new AHBuilder(ahandle)
        		.txId(tx.txId())
        		.build();
        
        ipfsCache.put(ahres);
        
        return ahandle;
    }

    @Override
    public AHandle unregisterAddress(Address owner) {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        assertArgumentHasLabel(owner);
        
        // Do nothing if not registered
        AHandle ahandle = findAddressRegistation(owner, null);
        if (ahandle == null) return null;
        
        Wallet wallet = getBlockchain().getWallet();
        List<UTXO> utxos = wallet.listLockUnspent(Arrays.asList(owner)).stream()
                .filter(utxo -> ahmgr.isOurs(wallet.getTransaction(utxo.getTxId())))
                .peek(utxo -> wallet.lockUnspent(utxo, true))
                .collect(Collectors.toList());

        utxos = addMoreUtxoIfRequired(owner, utxos);
        
        String changeAddr = wallet.getChangeAddress(owner.getLabels().get(0)).getAddress();
        String txId = wallet.sendToAddress(changeAddr, changeAddr, Wallet.ALL_FUNDS, utxos);
        
        if (txId == null) {
            LOG.warn("Cannot unregister PubKey: {} => {}", owner, ahandle);
            return ahandle;
        }
        
        Multihash cid = ahandle.getCid();
        LOG.info("Unregister PubKey: {} => Tx {} => {}", owner.getAddress(), txId, cid);
        
        AHandle ahres = new AHBuilder(ahandle)
        		.pubKey(null)
        		.build();
        
        return ahres;
    }

    @Override
    public List<Multihash> unregisterIpfsContent(Address owner, List<Multihash> cids) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        assertArgumentHasLabel(owner);

        List<Multihash> results = new ArrayList<>();
        
        Wallet wallet = getBlockchain().getWallet();
        List<UTXO> utxos = wallet.listLockUnspent(Arrays.asList(owner)).stream()
                .filter(utxo -> {
                	
                    FHandle fh = fhmgr.getHandleFromTx(owner, utxo);
                    if (fh == null) return false;
                    
                    if (cids == null || cids.contains(fh.getCid())) {
                        results.add(fh.getCid());
                        return true;
                    }
                    
                    return false;
                })
                .peek(utxo -> wallet.lockUnspent(utxo, true))
                .collect(Collectors.toList());
                
        utxos = addMoreUtxoIfRequired(owner, utxos);
        
        String changeAddr = wallet.getChangeAddress(owner.getLabels().get(0)).getAddress();
        String txId = wallet.sendToAddress(changeAddr, changeAddr, Wallet.ALL_FUNDS, utxos);
        
        if (txId == null) {
            LOG.warn("Cannot unregister IPFS: {} => {}", owner, results);
            return results;
        }
        
        synchronized (ipfsCache) {
            results.forEach(cid -> {
                LOG.info("Unregister IPFS: {} => {}", cid, txId);
                ipfsCache.remove(cid, FHandle.class);
            });
        }
        
        return results;
    }

    @Override
    public FHandle addIpfsContent(Address owner, Path path, URL srcUrl) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(path, "Null path");
        AssertArgument.assertNotNull(srcUrl, "Null srcUrl");
        
        return addIpfsContent(owner, path, srcUrl.openStream());
    }

    @Override
    public FHandle addIpfsContent(Address owner, Path dstPath, InputStream input) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(dstPath, "Null path");
        AssertArgument.assertNotNull(input, "Null input");
    	
        return addIpfsContent(owner, dstPath, input, false);
    }
    
    public FHandle addIpfsContent(Address owner, Path dstPath, InputStream input, boolean dryRun) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(dstPath, "Null path");
        AssertArgument.assertNotNull(input, "Null input");
        
        assertArgumentHasPrivateKey(owner);
        
        boolean fileOverwrite = config.isOverwrite();
        Path plainPath = assertValidPlainPath(owner, dstPath, false);
        NessusUserFault.assertTrue(fileOverwrite || !plainPath.toFile().exists(), "Local content already exists: " + dstPath);
        
        mkdirs(plainPath.getParent());
        Files.copy(input, plainPath, StandardCopyOption.REPLACE_EXISTING);
        
        return addIpfsContent(owner, dstPath, dryRun);
    }

    @Override
    public FHandle addIpfsContent(Address owner, Path srcPath) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(srcPath, "Null srcPath");
    	
        return addIpfsContent(owner, srcPath, false);
    }
    
    public FHandle addIpfsContent(Address owner, Path srcPath, boolean dryRun) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(srcPath, "Null srcPath");
        
        assertArgumentHasPrivateKey(owner);
        
        PublicKey pubKey = assertAddressRegistration(owner);
        
        LOG.info("Start IPFS Add: {} {}", owner, srcPath);
        
        FHandle fhandle = buildTreeFromPath(owner, srcPath);
        
        LOG.info("IPFS encrypt: {}", fhandle.toString(true));
        
        fhandle = encrypt(owner, fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle.toString(true));
        
        Path tmpPath = fhandle.getFilePath();
        AssertState.assertTrue(tmpPath.toFile().exists(), "Encrypted content does not exists: " + tmpPath);
        
        fhandle = fhmgr.addIpfsContent(fhandle, dryRun);
        AssertState.assertNotNull(fhandle.getCid(), "No ipfs content ids");
        
        // Move the temp file to its crypt path
        
        Multihash cid = fhandle.getCid();
        Path fullPath = getCryptPath(owner).resolve(cid.toBase58());
        FileUtils.atomicMove(tmpPath, fullPath);
        URL furl = fullPath.toUri().toURL();
        
        // Check if this content is already known
        
        FHandle fhres = fhmgr.getUnspentHandle(owner, cid, FHandle.class);
        if (fhres != null) {
        	
        	if (!fhres.isAvailable()) {
        		
        		fhres = new FHBuilder(fhres)
        				.url(furl)
        				.build();
        		
        		fhres = fhmgr.createFHandleTree(fhres);
        	}
        	
            LOG.info("IPFS duplicate: {}", fhres);
            
        } else {
        	
            fhres = new FHBuilder(fhandle)
                    .url(furl)
                    .cid(cid)
                    .build();
            
            LOG.info("IPFS record: {}", fhres);
            
            fhres = recordFileData(owner, fhres);
            
        }
        
        fhres = FHWalker.walkTree(fhres, new Visitor() {

            @Override
            public FHandle visit(FHandle fhaux) throws IOException {
                
                Path path = fhaux.getPath();
                FHandle fhroot = fhaux.getRoot();
                Path rootPath = fhroot.getFilePath();
                
                Path relPath = fhroot.getPath().relativize(path);
                Path fullPath = rootPath.resolve(relPath);
                URL furl = fullPath.toUri().toURL();
                
                FHandle fhres = new FHBuilder(fhroot)
                        .findChild(path)
                        .available(true)
                        .url(furl)
                        .build();
                
                return fhres;
            }
        });
        
        ipfsCache.put(fhres);
        
        LOG.info("Done IPFS Add: {}", fhres.toString(true));
        
        return fhres;
    }

    public FHandle buildTreeFromPath(Address owner, Path path) throws IOException {
        
        Path plainPath = assertValidPlainPath(owner, path, false);
        AssertState.assertTrue(plainPath.toFile().exists(), "Local content does not exists: " + plainPath);
        
        boolean isDirectory = plainPath.toFile().isDirectory();
        Path rootPath = isDirectory ? plainPath.getParent() : plainPath;
        
        List<FHandle> fhandles = new ArrayList<>();
        Stack<FHandle> fhstack = new Stack<>();
        
        Files.walkFileTree(plainPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                FHandle fhandle = createFHandle(dir);
                fhandles.add(fhandle);
                fhstack.push(fhandle);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                fhstack.pop();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FHandle fhandle = createFHandle(file);
                fhandles.add(fhandle);
                return FileVisitResult.CONTINUE;
            }

            FHandle createFHandle(Path fullPath) throws IOException {
                
                FHandle parent = !fhstack.isEmpty() ? fhstack.peek() : null;
                URL furl = fullPath.toFile().toURI().toURL();
                boolean isDirectory = plainPath.toFile().isDirectory();
                Path relPath = isDirectory ? rootPath.relativize(fullPath) : path;
                
                FHandle fhres = new FHBuilder(owner, relPath, furl)
                        .parent(parent)
                        .available(true)
                        .build();
                
                return fhres;
            }
        });

        AssertState.assertTrue(!fhandles.isEmpty(), "Cannot obtain fhandle for: " + path);
        FHandle fhres = fhandles.get(0);
        
        return fhres;
    }

    @Override
    public FHandle getIpfsContent(Address owner, Multihash cid, Path path, Long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(cid, "Null cid");

        assertArgumentHasPrivateKey(owner);

        LOG.info("Start IPFS Get: {} {}", owner, cid);
        
        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        FHandle fhandle = ipfsGet(owner, cid, timeout);
        
        LOG.info("IPFS decrypt: {}", fhandle);
        
        fhandle = decrypt(fhandle, path, true);
        
        LOG.info("Done IPFS Get: {}", fhandle);
        
        return fhandle;
    }
    
    @Override
    public FHandle sendIpfsContent(Address owner, Multihash cid, Address toAddr, Long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(toAddr, "Null toAddr");
        AssertArgument.assertNotNull(cid, "Null cid");
        
        assertArgumentHasLabel(owner);
        assertArgumentHasPrivateKey(owner);
        assertArgumentNotChangeAddress(owner);
        
        PublicKey pubKey = assertAddressRegistration(toAddr);
        
        LOG.info("Start IPFS Send: {} {} => {}", owner, cid, toAddr);
        
        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        FHandle fhandle = ipfsGet(owner, cid, timeout);
        
        LOG.info("IPFS decrypt: {}", fhandle.toString(true));

        fhandle = decrypt(fhandle, null, false);

        FHandle fhres = new FHBuilder(fhandle)
        		.secretToken(null)
                .owner(toAddr)
                .cid(null)
                .build();

        LOG.info("IPFS encrypt: {}", fhres);
        
        fhres = encrypt(owner, fhres, pubKey);
        Path tmpPath = fhres.getFilePath();
        
        LOG.info("IPFS add: {}", fhres.toString(true));
        
        fhres = fhmgr.addIpfsContent(fhres, false);
        
        Path cryptPath = getCryptPath(toAddr).resolve(cid.toBase58());
        FileUtils.atomicMove(tmpPath, cryptPath);

        URL furl = cryptPath.toUri().toURL();
        fhres = new FHBuilder(fhres)
                .url(furl)
                .build();
        
        LOG.info("IPFS record: {}", fhres.toString(true));
        
        fhres = recordFileData(owner, fhres);
        
        LOG.info("Done IPFS Send: {}", fhres.toString(true));
        
        return fhres;
    }

    @Override
    public AHandle findAddressRegistation(Address owner, Long timeout) {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        AHandle ahandle = ahmgr.findContentAsync(owner, timeout);
        
        return ahandle;
    }

    @Override
    public List<FHandle> findIpfsContent(Address owner, Long timeout) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");

        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        List<FHandle> fhandles = fhmgr.findContentAsync(owner, timeout);
        
        return fhandles;
    }

    @Override
    public List<FHandle> findLocalContent(Address owner) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        List<FHandle> fhandles = new ArrayList<>();
        
        Path plainPath = getPlainPath(owner);
        if (plainPath.toFile().exists()) {
            for (File file : plainPath.toFile().listFiles()) {
                Path path = plainPath.relativize(file.toPath());
                fhandles.add(findLocalContent(owner, path));
            }
        }
        
        return fhandles;
    }

    @Override
    public FHandle findLocalContent(Address owner, Path path) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(path, "Null path");

        Path plainPath = getPlainPath(owner).resolve(path);
        if (!plainPath.toFile().exists()) return null;
        
        FHandle fhres;
        
        if (plainPath.toFile().isDirectory()) {
            fhres = buildTreeFromPath(owner, path);
        } else {
            URL furl = plainPath.toUri().toURL();
            fhres = new FHBuilder(owner, path, furl)
                    .available(true)
                    .build();
        }
        
        return fhres;
    }

    @Override
    public InputStream getLocalContent(Address owner, Path path) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(path, "Null path");
        
        Path plainPath = assertValidPlainPath(owner, path, false);
        if (!plainPath.toFile().isFile())
            return null;

        return new FileInputStream(plainPath.toFile());
    }

    @Override
    public boolean removeLocalContent(Address owner, Path path) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        path = path != null ? path : Paths.get("");
        Path plainPath = assertValidPlainPath(owner, path, true);
        
        boolean removed = FileUtils.recursiveDelete(plainPath);
        AssertState.assertTrue(removed, "Cannot remove: " + plainPath);
        
        Path parent = plainPath.getParent();
        while (parent != null && !getPlainPath(owner).equals(parent)) {
        	String[] childfiles = parent.toFile().list();
			if (childfiles != null && childfiles.length == 0) {
        		removed = FileUtils.recursiveDelete(parent);
                AssertState.assertTrue(removed, "Cannot remove: " + parent);
        	}
            parent = parent.getParent();
        }
        
        return !path.toFile().exists();
    }

    public BitcoindRpcClient getBitcoinRpcClient() {
        return ((RpcClientSupport) blockchain).getRpcClient();
    }

    public FHeaderValues getFHeaderValues() {
        return new FHeaderValues("Nessus", "1.0");
    }

    public Path getRootPath() {
        Path rootPath = config.getDataDir();
        return mkdirs(rootPath);
    }

    public Path getPlainPath(Address owner) {
        Path plainPath = getRootPath().resolve("plain").resolve(owner.getAddress());
        return mkdirs(plainPath);
    }

    public Path getCryptPath(Address owner) {
        Path cryptPath = getRootPath().resolve("crypt").resolve(owner.getAddress());
        return mkdirs(cryptPath);
    }

    public Path getTempPath() {
        Path tmpPath = getRootPath().resolve("tmp");
        return mkdirs(tmpPath);
    }

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory(getTempPath(), "");
    }
    
    private FHandle ipfsGet(Address owner, Multihash cid, long timeout) throws IOException, IPFSTimeoutException {
        
        FHandle fhandle = fhmgr.getUnspentHandle(owner, cid, FHandle.class);
        if (fhandle == null) 
        	return null;
        
        if (!fhandle.isAvailable())
        	fhandle = fhmgr.getIpfsContent(fhandle, timeout);
        
        return fhandle;
    }
    
    private FHandle recordFileData(Address owner, FHandle fhandle) throws GeneralSecurityException {

        AssertArgument.assertTrue(fhandle.isEncrypted(), "File not encrypted: " + fhandle);

        // Construct the OP_RETURN data

        byte[] data = fhmgr.createFileData(fhandle);

        // Send a Tx to record the OP_TOKEN data

        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = owner.getLabels().get(0);
        List<UTXO> utxos = wallet.selectUnspent(label, spendAmount.add(feePerKB));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(spendAmount.add(feePerKB));

        List<TxOutput> outputs = new ArrayList<>();
        if (dustAmount.compareTo(changeAmount) < 0) {
            outputs.add(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        
        Address toAddr = fhandle.getOwner();
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

            // Lock the UTXO
            
            List<UTXO> unlocked = fhmgr.listLockedAndUnlockedUnspent(owner, false, true);
            unlocked.stream()
            	.filter(utxo -> utxo.getTxId().equals(txId))
            	.filter(utxo -> utxo.getVout() == vout)
            	.forEach(utxo -> wallet.lockUnspent(utxo, false));
        }

        LOG.debug("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, owner);
        
        fhandle = new FHBuilder(fhandle)
                .txId(txId)
                .build();

        return fhandle;
    }

    private BigDecimal getUTXOAmount(List<UTXO> utxos) {
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    private FHandle encrypt(Address owner, FHandle fhandle, PublicKey pubKey) throws IOException, GeneralSecurityException {
        AssertArgument.assertTrue(!fhandle.isEncrypted(), "File already encrypted: " + fhandle);
        
        AESCipher aes = new AESCipher();
        RSACipher rsa = new RSACipher();
        
        // Get the CID for the plain content
        // DO NOT ACTUALLY ADD THIS TO IPFS (--hash-only)
        
        List<Multihash> cids = ipfsClient.add(fhandle.getFilePath(), true);
        AssertState.assertTrue(cids.size() > 0, "Cannot obtain content ids for: " + fhandle);
        Multihash cid = cids.get(cids.size() - 1);
        
        // Get the AES secret key for the entire tree
        SecretKey secKey = AESUtils.newSecretKey(owner, cid);
        
        // Encrypt the AES secret key
        byte[] tokBytes = rsa.encrypt(pubKey, secKey.getEncoded());
        String secToken = Base64.getEncoder().encodeToString(tokBytes);

        Path tmpDir = createTempDir();
        FHandle fhres = FHWalker.walkTree(fhandle, new Visitor() {

            @Override
            public FHandle visit(FHandle fhaux) throws IOException, GeneralSecurityException {

                Path path = fhaux.getPath();
                Path tmpPath = tmpDir.resolve(path);
                mkdirs(tmpPath.getParent());
                
                FHandle fhres = new FHBuilder(fhaux.getRoot())
                        .findChild(path)
                        .url(tmpPath.toUri().toURL())
                        .secretToken(secToken)
                        .build();
                
                if (fhres.hasChildren()) 
                    return fhres;
                
                File srcFile = fhaux.getFilePath().toFile();
                AssertState.assertTrue(srcFile.isFile(), "Cannot obtain source file: " + srcFile);
                
                // Get the CID for the plain content
                // DO NOT ACTUALLY ADD THIS TO IPFS (--hash-only)
                
                Multihash cid = ipfsClient.addSingle(fhaux.getFilePath(), true);
                
                // Create a content based AES key & IV
                byte[] iv = AESUtils.getIV(owner, cid);
                
                try (FileWriter fw = new FileWriter(tmpPath.toFile())) {
                    
                    // Write the file header
                    FHeader header = FHeader.fromFHandle(fhvals, fhres);
                    header.write(fw);
                    
                    try (InputStream ins = new FileInputStream(srcFile)) {
                        
                        // Encrypt the file content
                        InputStream encrypted = aes.encrypt(secKey, iv, ins, null);
                        
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        StreamUtils.copyStream(encrypted, baos);
                        String base64Encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
                        
                        // Append encrypted file content
                        fw.write(base64Encoded);
                        
                    } catch (GeneralSecurityException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
                
                return fhres;
            }
        });
        
        return fhres;
    }

    public FHandle decrypt(FHandle fhandle, Path dstPath, boolean storePlain) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");

        AESCipher aes = new AESCipher();
        RSACipher rsa = new RSACipher();
        
        Address owner = fhandle.getOwner();
        KeyPair keyPair = RSAUtils.newKeyPair(owner);
        PrivateKey privKey = keyPair.getPrivate();
        
        Path tmpDir = createTempDir();
        FHandle fhres = FHWalker.walkTree(fhandle, new Visitor() {

            @Override
            public FHandle visit(FHandle fhandle) throws IOException {

                Path path = fhandle.getPath();
                Path tmpPath = tmpDir.resolve(path);
                mkdirs(tmpPath.getParent());
                
                FHandle fhres = new FHBuilder(fhandle.getRoot())
                        .findChild(path)
                        .url(tmpPath.toUri().toURL())
                        .build();
                
                if (fhandle.hasChildren()) 
                    return fhres;
                
                // Read the file header
                
                File srcFile = fhandle.getFilePath().toFile();
                AssertState.assertTrue(srcFile.isFile(), "Cannot obtain source file: " + srcFile);
                
                FHeader header;
                try (FileReader fr = new FileReader(srcFile)) {
                    header = FHeader.fromReader(fhvals, fr);
                }
                
                // Read the content
                try (FileReader fr = new FileReader(srcFile)) {

                    // Skip the header
                    for (int i = 0; i < header.length; i++) {
                        fr.read();
                    }
                    
                    byte[] encToken = Base64.getDecoder().decode(header.token);
                    byte[] token = rsa.decrypt(privKey, encToken);
                    SecretKey secKey = AESUtils.decodeSecretKey(token);

                    // Read Base64 encoded content
                    String base64Encoded = new BufferedReader(fr).readLine();
                    byte[] encBytes = Base64.getDecoder().decode(base64Encoded);
                    ByteArrayInputStream ins = new ByteArrayInputStream(encBytes);
                    
                    // Decrypt the file content
                    InputStream decrypted = aes.decrypt(secKey, ins);
                    
                    // [TODO] How is it possible that the tmp file already exists?
                    Path tmpFile = tmpDir.resolve(fhres.getPath());
                    mkdirs(tmpFile.getParent());

                    Files.copy(decrypted, tmpFile, StandardCopyOption.REPLACE_EXISTING); 
                    
                    fhres = new FHBuilder(fhres.getRoot())
                            .findChild(path)
                            .url(tmpFile.toUri().toURL())
                            .secretToken(null)
                            .build();
                    
                } catch (GeneralSecurityException ex) {
                    throw new IllegalStateException(ex);
                }
                
                return fhres;
            }
        });
                
        if (storePlain) {
            
            dstPath = dstPath != null ? dstPath : fhandle.getPath();
            AssertArgument.assertTrue(!dstPath.isAbsolute(), "Given path must be relative: " + dstPath);
            
            boolean fileOverwrite = config.isOverwrite();
            Path plainPath = assertValidPlainPath(owner, dstPath, false);
            NessusUserFault.assertTrue(fileOverwrite || !plainPath.toFile().exists(), "Local content already exists: " + dstPath);
            
            Path tmpPath = fhres.getFilePath();
            mkdirs(plainPath.getParent());
            
            // Cannot use atomic move because of potential cross device link
            FileUtils.recursiveDelete(plainPath);
            FileUtils.recursiveCopy(tmpPath, plainPath);
            FileUtils.recursiveDelete(tmpPath);
            
            FHandle fhaux = buildTreeFromPath(owner, dstPath);
            fhres = new FHBuilder(fhaux)
            		.cid(fhres.getCid())
            		.attempt(fhres.getAttempt())
            		.elapsed(fhres.getElapsed())
            		.build();
        }
        
        return fhres;
    }
    
    // Make sure the total amount in the utxos is > required fees
    private List<UTXO> addMoreUtxoIfRequired(Address addr, List<UTXO> utxos) {
        
        // Nothing to do if amount > min fees 
        BigDecimal amount = AbstractWallet.getUTXOAmount(utxos);
        if (amount.compareTo(network.getMinTxFee()) > 0) return utxos;
        
        List<UTXO> result = new ArrayList<>(utxos);
        LOG.info("Utxos amount: {}", amount);
        
        List<UTXO> unspent = wallet.listUnspent(Arrays.asList(addr));
        LOG.info("All unspent: {}", unspent);
        
        for (UTXO aux : unspent) {
            if (!result.contains(aux)) {
                result.add(aux);
                amount = AbstractWallet.getUTXOAmount(result);
                LOG.info("Utxos amount: {}", amount);
                if (amount.compareTo(network.getMinTxFee()) > 0) 
                    break;
            } 
        }
        
        return result;
    }
    
	private Path mkdirs(Path path) {
		if (path.toFile().isDirectory()) return path;
		AssertState.assertFalse(path.toFile().isFile(), "File already exists: " + path);
		AssertState.assertTrue(path.toFile().mkdirs(), "Cannot create directory: " + path);
		return path;
	}
	
    private void assertArgumentHasPrivateKey(Address addr) {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);
    }

    private void assertArgumentHasLabel(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().isEmpty(), "Address has no label: " + addr);
    }
    
    private void assertArgumentNotChangeAddress(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().contains(Wallet.LABEL_CHANGE), "Cannot use change address: " + addr);
    }
    
	private PublicKey assertAddressRegistration(Address addr) {
		AHandle ahandle = findAddressRegistation(addr, null);
        AssertArgument.assertTrue(ahandle != null && ahandle.isAvailable(), "Cannot obtain encryption key for: " + addr);
		return ahandle.getPubKey();
	}

    private Path assertValidPlainPath(Address owner, Path path, boolean allowEmpty) {
        
        AssertArgument.assertNotNull(path, "Null path");
        AssertArgument.assertTrue(!path.isAbsolute(), "Not a relative path: " + path);
        
        String pstr = path.toString();
        AssertArgument.assertTrue(allowEmpty || pstr.trim().length() > 0, "Empty path");
        
        return getPlainPath(owner).resolve(path);
    }
}