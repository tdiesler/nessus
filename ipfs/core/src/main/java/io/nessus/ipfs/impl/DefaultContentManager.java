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
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.FHandle.FHReference;
import io.nessus.ipfs.FHandle.Visitor;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.ipfs.MerkleNotFoundException;
import io.nessus.ipfs.NessusUserFault;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.FileUtils;
import io.nessus.utils.StreamUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class DefaultContentManager implements ContentManager {

    static final Logger LOG = LoggerFactory.getLogger(DefaultContentManager.class);

    public static final long DEFAULT_IPFS_TIMEOUT = 6000; // 6 sec
    public static final int DEFAULT_IPFS_ATTEMPTS = 100; // 10 min
    public static final int DEFAULT_IPFS_THREADS = 12;

    protected final ContentManagerConfig config;
    protected final IPFSClient ipfsClient;
    protected final Blockchain blockchain;
    protected final Network network;
    protected final Wallet wallet;
    protected final FHeaderValues fhvals;
    private final BCData bcdata;

    // Executor service for async IPFS get operations
    protected final ExecutorService executorService;
    
    // Contains fully initialized FHandles pointing to encrypted files.
    // It is guarantied that the wallet contains the privKeys needed to access these files.
    //
    // Storing a renduandant copy of content that is primarily managed in IPFS
    // is somewhat problematic. Because the here stored encrypted files are not subject 
    // to IPFS eviction policy, the lists may diverge possibly resulting in breakage of "show" on the gateway.
    // However, not all the information needed by this app (e.g. path) can be stored on the blockchain.
    // Hence, an IPFS get is needed in any case to get at the IPFS file header. For large files this may result
    // in an undesired performance hit. We may need to find ways to separate this metadata from the actual content.
    private final IPFSFileCache filecache = new IPFSFileCache();
    
    public DefaultContentManager(ContentManagerConfig config) {
        this.config = config.makeImmutable();

        ipfsClient = config.getIpfsClient();
        blockchain = config.getBlockchain();
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        fhvals = getFHeaderValues();
        bcdata = new BCData(fhvals);
        
        LOG.info("{}{}", getClass().getSimpleName(), config);
        
        int ipfsThreads = config.getIpfsThreads();
        executorService = Executors.newFixedThreadPool(ipfsThreads, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
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

    @Override
    public PublicKey registerAddress(Address addr) throws GeneralSecurityException, IOException {
        AssertArgument.assertNotNull(addr, "Null addr");
        
        assertArgumentHasLabel(addr);
        assertArgumentHasPrivateKey(addr);
        assertArgumentNotChangeAddress(addr);

        // Do nothing if already registered
        PublicKey pubKey = findAddressRegistation(addr);
        if (pubKey != null)
            return pubKey;

        // Store the EC key, which is derived from the privKey

        KeyPair keyPair = RSAUtils.newKeyPair(addr);
        pubKey = keyPair.getPublic();
        
        AddrRegistration areg = new AddrRegistration(fhvals, addr, pubKey);
        String cid = areg.addIpfsContent(ipfsClient);
        
        byte[] data = bcdata.createAddrData(Multihash.fromBase58(cid));

        // Send a Tx to record the OP_RETURN data

        Network network = getBlockchain().getNetwork();
        Wallet wallet = getBlockchain().getWallet();
        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = areg.getLabel();
        List<UTXO> utxos = wallet.selectUnspent(label, spendAmount.add(feePerKB));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(spendAmount.add(feePerKB));

        List<TxOutput> outputs = new ArrayList<>();
        if (dustAmount.compareTo(changeAmount) < 0) {
            outputs.add(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        outputs.add(new TxOutput(addr.getAddress(), dataAmount, data));
            
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .outputs(outputs)
                .build();

        String txId = wallet.sendTx(tx);

        LOG.info("Register PubKey: {} => Tx {} => {}", addr.getAddress(), txId, cid);

        tx = wallet.getTransaction(txId);
        int vout = tx.outputs().size() - 2;
        TxOutput dataOut = tx.outputs().get(vout);
        AssertState.assertEquals(addr.getAddress(), dataOut.getAddress());
        AssertState.assertEquals(dataAmount, dataOut.getAmount());

        LOG.info("Lock unspent: {} {}", txId, vout);
        
        BitcoindRpcClient client = getRpcClient();
        client.lockUnspent(false, txId, vout);
        
        LOG.debug("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, addr);
        
        return pubKey;
    }

    @Override
    public PublicKey unregisterAddress(Address addr) {
        AssertArgument.assertNotNull(addr, "Null addr");
        
        assertArgumentHasLabel(addr);
        
        // Do nothing if not registered
        PublicKey pubKey = findAddressRegistation(addr);
        if (pubKey == null) return null;
        
        Wallet wallet = getBlockchain().getWallet();
        List<UTXO> utxos = wallet.listLockUnspent(Arrays.asList(addr)).stream()
                .filter(utxo -> pubKey.equals(getPubKeyFromTx(utxo, addr)))
                .peek(utxo -> wallet.lockUnspent(utxo, true))
                .collect(Collectors.toList());

        utxos = addMoreUtxoIfRequired(addr, utxos);
        
        String changeAddr = wallet.getChangeAddress(addr.getLabels().get(0)).getAddress();
        String txId = wallet.sendToAddress(changeAddr, changeAddr, Wallet.ALL_FUNDS, utxos);
        
        if (txId == null) {
            LOG.warn("Cannot unregister PubKey: {} => {}", addr, pubKey);
            return null;
        }
        
        LOG.info("Unregister PubKey: {} => {} => Tx {}", addr.getAddress(), pubKey, txId);
        
        return pubKey;
    }

    @Override
    public List<String> unregisterIpfsContent(Address owner, List<String> cids) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        assertArgumentHasLabel(owner);

        List<String> results = new ArrayList<>();
        
        Wallet wallet = getBlockchain().getWallet();
        List<UTXO> utxos = wallet.listLockUnspent(Arrays.asList(owner)).stream()
                .filter(utxo -> {
                    FHandle fh = getFHandleFromTx(owner, utxo);
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
        
        synchronized (filecache) {
            results.forEach(cid -> {
                LOG.info("Unregister IPFS: {} => {}", cid, txId);
                filecache.remove(cid);
            });
        }
        
        return results;
    }

    protected void clearFileCache() {
        filecache.clear();
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
        
        assertArgumentHasPrivateKey(owner);
        
        boolean replaceExisting = config.isReplaceExisting();
        Path plainPath = assertValidPlainPath(owner, dstPath);
        NessusUserFault.assertTrue(replaceExisting || !plainPath.toFile().exists(), "Local content already exists: " + dstPath);
        
        plainPath.getParent().toFile().mkdirs();
        Files.copy(input, plainPath, StandardCopyOption.REPLACE_EXISTING);
        
        return addIpfsContent(owner, dstPath);
    }

    @Override
    public FHandle addIpfsContent(Address owner, Path srcPath) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(srcPath, "Null srcPath");
        
        assertArgumentHasPrivateKey(owner);
        
        PublicKey pubKey = findAddressRegistation(owner);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + owner);
        
        LOG.info("Start IPFS Add: {} {}", owner, srcPath);
        
        FHandle fhandle = buildTreeFromPath(owner, srcPath);
        
        LOG.info("IPFS encrypt: {}", fhandle.toString(true));
        
        fhandle = encrypt(fhandle, pubKey);
        
        Path auxPath = fhandle.getFilePath();
        AssertState.assertTrue(auxPath.toFile().exists(), "Encrypted content does not exists: " + srcPath);
        
        LOG.info("IPFS add: {}", fhandle.toString(true));
        
        List<String> cids = ipfsClient.add(auxPath);
        AssertState.assertTrue(cids.size() > 0, "No ipfs content ids");
        
        // Move the temp file to its crypt path
        
        String cid = cids.get(cids.size() - 1);
        Path fullPath = getCryptPath(owner).resolve(cid);
        Files.move(auxPath, fullPath, StandardCopyOption.ATOMIC_MOVE);
        
        URL furl = fullPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("IPFS record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, owner);
        
        FHandle fhres = FHandle.walkTree(fhandle, new Visitor() {

            @Override
            public FHandle visit(FHandle fhandle) throws IOException {
                
                Path path = fhandle.getPath();
                FHandle fhroot = fhandle.getRoot();
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
        
        filecache.put(fhres);
        
        LOG.info("Done IPFS Add: {}", fhres.toString(true));
        
        return fhres;
    }

    protected FHandle buildTreeFromPath(Address owner, Path path) throws IOException {
        
        Path plainPath = assertValidPlainPath(owner, path);
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
    public FHandle getIpfsContent(Address owner, String cid, Path path, Long timeout) throws IOException, GeneralSecurityException {
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
    public FHandle sendIpfsContent(Address owner, String cid, Address target, Long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(cid, "Null cid");
        AssertArgument.assertNotNull(target, "Null target");
        
        assertArgumentHasLabel(owner);
        assertArgumentHasPrivateKey(owner);
        assertArgumentNotChangeAddress(owner);
        
        PublicKey pubKey = findAddressRegistation(target);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + target);
        
        LOG.info("Start IPFS Send: {} {}", owner, cid);
        
        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        FHandle fhandle = ipfsGet(owner, cid, timeout);
        
        LOG.info("IPFS decrypt: {}", fhandle);

        fhandle = decrypt(fhandle, null, false);

        fhandle = new FHBuilder(fhandle)
                .owner(target)
                .cid(null)
                .build();

        LOG.info("IPFS encrypt: {}", fhandle);
        
        fhandle = encrypt(fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle);
        
        Path tmpPath = fhandle.getFilePath();
        cid = ipfsClient.addSingle(tmpPath);
        
        Path cryptPath = getCryptPath(target).resolve(cid);
        Files.move(tmpPath, cryptPath);

        URL furl = cryptPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("IPFS record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, target);
        
        LOG.info("Done IPFS Send: {}", fhandle);
        
        return fhandle;
    }

    @Override
    public PublicKey findAddressRegistation(Address addr) {
        AssertArgument.assertNotNull(addr, "Null addr");
        
        // We used to have a cache of these pubKey registrations.
        // This is no longer the case, because we want the owner 
        // to have full control over the registration. If the 
        // registration UTXO gets spent, it will immediately
        // no longer be available through this method.  
        
        PublicKey pubKey = null;
        
        List<UTXO> locked = listLockedAndUnlockedUnspent(addr, true, false);
        List<UTXO> allUnspent = listLockedAndUnlockedUnspent(addr, true, true);
        
        for (UTXO utxo : allUnspent) {
            
            String txId = utxo.getTxId();
            Tx tx = wallet.getTransaction(txId);
            
            pubKey = getPubKeyFromTx(utxo, addr);
            if (pubKey != null) {
                
                // The lock state of a registration may get lost due to wallet 
                // restart. Here we recreate that lock state if the given
                // address owns the registration
                
                if (!locked.contains(utxo) && addr.getPrivKey() != null) {
                    
                    int vout = tx.outputs().size() - 2;
                    TxOutput dataOut = tx.outputs().get(vout);
                    AssertState.assertEquals(addr.getAddress(), dataOut.getAddress());
                    
                    LOG.info("Lock unspent: {} {}", txId, vout);
                    
                    wallet.lockUnspent(utxo, false);
                }
                
                break;
            }
        }
        
        return pubKey;
    }

    @Override
    public List<FHandle> findIpfsContent(Address owner, Long timeout) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");

        // The list of files that are recorded and unspent
        List<FHandle> unspentFHandles = new ArrayList<>();
        
        synchronized (filecache) {
        
            List<UTXO> locked = listLockedAndUnlockedUnspent(owner, true, false);
            List<UTXO> unspent = listLockedAndUnlockedUnspent(owner, true, true);
            
            for (UTXO utxo : unspent) {
                
                String txId = utxo.getTxId();
                Tx tx = wallet.getTransaction(txId);
                
                FHandle fhandle = getFHandleFromTx(owner, utxo);
                if (fhandle != null) {
                    
                    unspentFHandles.add(fhandle);
                    
                    // The lock state of a registration may get lost due to wallet 
                    // restart. Here we recreate that lock state if the given
                    // address owns the registration
                    
                    if (!locked.contains(utxo) && owner.getPrivKey() != null) {
                        
                        int vout = tx.outputs().size() - 2;
                        TxOutput dataOut = tx.outputs().get(vout);
                        AssertState.assertEquals(owner.getAddress(), dataOut.getAddress());
                        
                        LOG.info("Lock unspent: {} {}", txId, vout);
                        
                        wallet.lockUnspent(utxo, false);
                    }
                }
            }
            
            // Cleanup the file cache by removing entries that are no longer unspent
            List<String> cids = unspentFHandles.stream().map(fh -> fh.getCid()).collect(Collectors.toList());
            for (String cid : new HashSet<>(filecache.keySet())) {
                FHandle aux = filecache.get(cid);
                if (owner.equals(aux.getOwner()) && !cids.contains(aux.getCid())) {
                    filecache.remove(cid);
                }
            }
        }
        
        // Finally iterate get the IPFS files asynchronously
        timeout = timeout != null ? timeout : config.getIpfsTimeout();
        List<FHandle> results = ipfsGetAsync(unspentFHandles, timeout);
        
        return results;
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
        
        Path plainPath = assertValidPlainPath(owner, path);
        if (!plainPath.toFile().isFile())
            return null;

        return new FileInputStream(plainPath.toFile());
    }

    @Override
    public boolean removeLocalContent(Address owner, Path path) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(path, "Null path");
        
        Path plainPath = assertValidPlainPath(owner, path);
        boolean removed = FileUtils.recursiveDelete(plainPath);
        AssertState.assertTrue(removed, "Cannot remove: " + plainPath);
        
        Path parent = plainPath.getParent();
        File pfile = parent != null ? parent.toFile() : null;
        while (pfile != null && pfile.exists() && pfile.list().length == 0) {
            removed = FileUtils.recursiveDelete(pfile.toPath());
            AssertState.assertTrue(removed, "Cannot remove: " + pfile);
            pfile = pfile.getParentFile();
        }
        
        return !path.toFile().exists();
    }

    protected BitcoindRpcClient getRpcClient() {
        return ((RpcClientSupport) blockchain).getRpcClient();
    }

    protected FHeaderValues getFHeaderValues() {
        return new FHeaderValues("Nessus", "1.0");
    }

    Path getRootPath() {
        Path rootPath = config.getRootPath();
        rootPath.toFile().mkdirs();
        return rootPath;
    }

    Path getPlainPath(Address owner) {
        Path plainPath = getRootPath().resolve("plain").resolve(owner.getAddress());
        plainPath.toFile().mkdirs();
        return plainPath;
    }

    Path getCryptPath(Address owner) {
        Path cryptPath = getRootPath().resolve("crypt").resolve(owner.getAddress());
        cryptPath.toFile().mkdirs();
        return cryptPath;
    }

    Path getTempPath() {
        Path tmpPath = getRootPath().resolve("tmp");
        tmpPath.toFile().mkdirs();
        return tmpPath;
    }

    Path createTempDir() throws IOException {
        return Files.createTempDirectory(getTempPath(), "");
    }
    
    Path createTempFile() throws IOException {
        return Files.createTempFile(getTempPath(), "", "");
    }
    
    private FHandle ipfsGet(Address owner, String cid, long timeout) throws IOException, IPFSTimeoutException {
        
        FHandle fhandle;
        synchronized (filecache) {
            
            fhandle = filecache.get(cid);
            if (fhandle != null && !fhandle.isMissing())
                return fhandle;
            
            if (fhandle == null) {
                fhandle = new FHBuilder(owner, cid)
                        .elapsed(0L)
                        .build();
                filecache.put(fhandle);
            }
        }
        
        return ipfsGet(fhandle, timeout);
    }
    
    private FHandle ipfsGet(FHandle fhandle, long timeout) throws IOException, IPFSTimeoutException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");
        AssertArgument.assertNotNull(fhandle.getOwner(), "Null owner");
        AssertArgument.assertNotNull(fhandle.getCid(), "Null cid");
        
        Address owner = fhandle.getOwner();
        String cid = fhandle.getCid();
        
        Path cryptPath = getCryptPath(owner);
        Path rootPath = cryptPath.resolve(cid);
        
        // Fetch the content from IPFS
        
        if (!rootPath.toFile().exists()) {
            
            long before = System.currentTimeMillis();
            try {
                
                Future<Path> future = ipfsClient.get(cid, cryptPath);
                Path resPath = future.get(timeout, TimeUnit.MILLISECONDS);
                
                AssertState.assertEquals(rootPath, resPath);
                
            } catch (InterruptedException | ExecutionException ex) {
                
                Throwable cause = ex.getCause();
                if (cause instanceof IPFSException) 
                    throw (IPFSException)cause;
                else 
                    throw new IPFSException(ex);
                
            } catch (TimeoutException ex) {
                
                throw new IPFSTimeoutException(ex);
                
            } finally {
                
                long elapsed = System.currentTimeMillis() - before;
                fhandle = new FHBuilder(fhandle)
                        .elapsed(elapsed)
                        .build();
                
                filecache.put(fhandle);
            }
        }
        
        if (fhandle.getURL() == null) {
            URL furl = rootPath.toUri().toURL();
            fhandle = new FHBuilder(fhandle).url(furl).build();
        }
        
        File rootFile = rootPath.toFile();
        AssertState.assertTrue(rootFile.exists(), "Cannot find IPFS content at: " + rootFile);
        
        if (rootFile.isDirectory()) {
            fhandle = createIPFSFileTree(fhandle);
        } else {
            fhandle = createFromFileHeader(null, fhandle);
        }
        
        FHandle fhres = new FHBuilder(fhandle)
                .available(true)
                .build();
        
        LOG.info("IPFS found: {}", fhres.toString(true));

        filecache.put(fhres);
        
        return fhres;
    }

    private FHandle createIPFSFileTree(FHandle fhandle) throws IOException {
        
        Stack<FHandle> fhstack = new Stack<>();
        
        // Find the root node path by reading the 
        // file header of the first encrypted file we find
        
        Path rootPath = fhandle.getFilePath();
        File rootFile = rootPath.toFile();
        AssertState.assertTrue(rootFile.exists(), "Cannot find IPFS content at: " + rootFile);
        
        FHReference fhref = new FHReference();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                
                URL furl = file.toUri().toURL();
                FHandle fhaux = new FHBuilder(fhandle)
                        .url(furl)
                        .build();
                
                FHandle fhres = createFromFileHeader(null, fhaux);
                
                Path path = fhres.getPath().getName(0);
                furl = rootFile.toURI().toURL();

                fhres = new FHBuilder(fhres)
                        .path(path)
                        .url(furl)
                        .build();
                
                fhref.setFHandle(fhres);
                
                return FileVisitResult.TERMINATE;
            }
        });
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                FHandle parent = !fhstack.isEmpty() ? fhstack.peek() : null;
                FHandle fhandle = createFileHandle(parent, dir);
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
                FHandle parent = !fhstack.isEmpty() ? fhstack.peek() : null;
                createFileHandle(parent, file);
                return FileVisitResult.CONTINUE;
            }
            
            FHandle createFileHandle(FHandle parent, Path fullPath) throws IOException {
                
                FHandle fhres;
                if (fullPath.toFile().isDirectory()) {
                    
                    if (parent == null) {
                        
                        // Root handle                         
                        fhres = fhref.getFHandle();
                        
                    } else {
                        
                        // Sub directory handles
                        
                        FHandle fhroot = fhref.getFHandle();
                        Path relPath = rootPath.relativize(fullPath);
                        relPath = fhroot.getPath().resolve(relPath);
                        URL furl = fullPath.toUri().toURL();
                        
                        fhres = new FHBuilder(fhandle)
                                .parent(parent)
                                .path(relPath)
                                .url(furl)
                                .build();
                    }
                    
                } else {
                    
                    URL furl = fullPath.toUri().toURL();
                    FHandle fhaux = new FHBuilder(fhandle)
                            .url(furl)
                            .build();

                    fhres = createFromFileHeader(parent, fhaux);
                }
                
                return fhres;
            }
        });
        
        FHandle fhres = fhref.getFHandle();
        AssertState.assertNotNull(fhres, "Cannot obtain fhandle for: " + rootFile);
        
        return fhres;
    }

    private FHandle createFromFileHeader(FHandle parent, FHandle fhandle) throws IOException {
        AssertArgument.assertNotNull(fhandle, "Null fullPath");
        
        Path fullPath = fhandle.getFilePath();
        AssertState.assertTrue(fullPath.toFile().isFile(), "Cannot find IPFS content at: " + fullPath);
        
        try (FileReader fr = new FileReader(fullPath.toFile())) {

            FHeader header = FHeader.fromReader(fhvals, fr);
            Address owner = findAddress(header.owner);
            String encToken = header.token;
            Path path = header.path;
            
            AssertState.assertNotNull(owner, "Address unknown to this wallet: " + header.owner);

            boolean available = parent != null ? parent.isAvailable() : false;
            
            FHandle fhres = new FHBuilder(fhandle)
                    .secretToken(encToken)
                    .available(available)
                    .parent(parent)
                    .path(path)
                    .build();
            
            return fhres;
        }
    }
    
    private List<FHandle> ipfsGetAsync(List<FHandle> fhandles, long timeout) throws IOException {
        
        synchronized (filecache) {
            for (FHandle fhaux : fhandles) {
                String cid = fhaux.getCid();
                FHandle fhc = filecache.get(cid);
                if (fhc == null) {
                    fhc = new FHBuilder(fhaux).elapsed(0L).build();
                    LOG.info("IPFS submit: {}", fhc);
                    filecache.put(fhc);
                }
            }
        }
        
        Future<List<FHandle>> future = executorService.submit(new Callable<List<FHandle>>() {

            @Override
            public List<FHandle> call() throws Exception {
                
                List<FHandle> missing = getMissingFHandles(fhandles);
                
                while(!missing.isEmpty()) {
                    for (FHandle fh : missing) {
                        if (fh.setScheduled(true)) {
                            AsyncGetCallable callable = new AsyncGetCallable(fh, timeout);
                            executorService.submit(callable);
                        }
                    }
                    Thread.sleep(500L);
                    missing = getMissingFHandles(fhandles);
                }
                
                return getCurrentFHandles(fhandles);
            }
        });
        
        List<FHandle> results;
        try {
            results = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        } catch (TimeoutException ex) {
            results = getCurrentFHandles(fhandles);
        }
        
        return results;
    }

    private List<FHandle> getMissingFHandles(List<FHandle> fhandles) {
        List<FHandle> result = getCurrentFHandles(fhandles).stream()
                .filter(fh -> fh.isMissing())
                .collect(Collectors.toList());
        return result;
    }
    
    private List<FHandle> getCurrentFHandles(List<FHandle> fhandles) {
        synchronized (filecache) {
            List<FHandle> result = fhandles.stream()
                .map(fh -> fh.getCid())
                .map(cid -> filecache.get(cid))
                .collect(Collectors.toList());
            return result;
        }
    }
    
    private FHandle recordFileData(FHandle fhandle, Address toAddr) throws GeneralSecurityException {

        AssertArgument.assertTrue(fhandle.isEncrypted(), "File not encrypted: " + fhandle);

        // Construct the OP_RETURN data

        byte[] data = bcdata.createFileData(fhandle);

        // Send a Tx to record the OP_TOKEN data

        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        Address owner = fhandle.getOwner();
        String label = owner.getLabels().get(0);
        List<UTXO> utxos = wallet.selectUnspent(label, spendAmount.add(feePerKB));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(label);
        BigDecimal changeAmount = utxosAmount.subtract(spendAmount.add(feePerKB));

        List<TxOutput> outputs = new ArrayList<>();
        if (dustAmount.compareTo(changeAmount) < 0) {
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

        LOG.debug("Redeem change: {}", changeAmount);
        ((AbstractWallet) wallet).redeemChange(label, owner);
        
        fhandle = new FHBuilder(fhandle)
                .txId(txId)
                .build();

        return fhandle;
    }

    private Address findAddress(String rawAddr) {
        Address addrs = wallet.findAddress(rawAddr);
        AssertState.assertNotNull(addrs, "Address not known to this wallet: " + rawAddr);
        return addrs;
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
        
        AESCipher aes = new AESCipher();
        RSACipher rsa = new RSACipher();
        
        // Get the CID for the plain content
        // DO NOT ACTUALLY ADD THIS TO IPFS (--hash-only)
        List<String> cids = ipfsClient.add(fhandle.getFilePath(), false, true);
        AssertState.assertTrue(cids.size() > 0, "Cannot obtain content ids for: " + fhandle);
        Multihash cid = Multihash.fromBase58(cids.get(cids.size() - 1));
        
        // Get the AES secret key for the entire tree
        Address owner = fhandle.getOwner();
        SecretKey secKey = AESUtils.newSecretKey(owner, cid);
        
        // Encrypt the AES secret key
        KeyPair keyPair = RSAUtils.newKeyPair(owner);
        byte[] tokBytes = rsa.encrypt(keyPair.getPublic(), secKey.getEncoded());
        String secToken = Base64.getEncoder().encodeToString(tokBytes);

        Path tmpDir = createTempDir();
        FHandle fhres = FHandle.walkTree(fhandle, new Visitor() {

            @Override
            public FHandle visit(FHandle fhaux) throws IOException, GeneralSecurityException {

                Path path = fhaux.getPath();
                Path tmpPath = tmpDir.resolve(path);
                tmpPath.getParent().toFile().mkdirs();
                
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
                String encid = ipfsClient.addSingle(fhaux.getFilePath(), false, true);
                Multihash cid = Multihash.fromBase58(encid);
                
                // Create a content based AES key & IV
                byte[] iv = AESUtils.getIV(cid, owner);
                
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

    protected FHandle decrypt(FHandle fhandle, Path dstPath, boolean storePlain) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");

        AESCipher aes = new AESCipher();
        RSACipher rsa = new RSACipher();
        
        Address owner = fhandle.getOwner();
        KeyPair keyPair = RSAUtils.newKeyPair(owner);
        PrivateKey privKey = keyPair.getPrivate();
        
        Path tmpDir = createTempDir();
        FHandle fhres = FHandle.walkTree(fhandle, new Visitor() {

            @Override
            public FHandle visit(FHandle fhandle) throws IOException {

                Path path = fhandle.getPath();
                Path tmpPath = tmpDir.resolve(path);
                tmpPath.getParent().toFile().mkdirs();
                
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
                    tmpFile.getParent().toFile().mkdirs();
                    
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
            
            boolean replaceExisting = config.isReplaceExisting();
            Path plainPath = assertValidPlainPath(owner, dstPath);
            NessusUserFault.assertTrue(replaceExisting || !plainPath.toFile().exists(), "Local content already exists: " + dstPath);
            
            Path tmpPath = fhres.getFilePath();
            plainPath.getParent().toFile().mkdirs();
            Files.move(tmpPath, plainPath, StandardCopyOption.REPLACE_EXISTING);
            
            fhres = buildTreeFromPath(owner, dstPath);
        }
        
        return fhres;
    }
    
    protected PublicKey getPubKeyFromTx(UTXO utxo, Address owner) {
        AssertArgument.assertNotNull(utxo, "Null utxo");

        Tx tx = wallet.getTransaction(utxo.getTxId());
        if (!isOurs(tx)) 
            return null;
        
        PublicKey pubKey = null;

        List<TxOutput> outs = tx.outputs();
        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect OP_ADDR_DATA
        byte[] txdata = out1.getData();
        Multihash cid = bcdata.extractAddrData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (cid != null && outAddr != null) {

            // Not owned by the given address
            if (owner != null && !owner.equals(outAddr)) return null;
            
            AddrRegistration areg;
            try {
                
            	areg = AddrRegistration.fromIpfs(ipfsClient, wallet, fhvals, cid);
            	AssertState.assertEquals(outAddr, areg.getAddress());
            	
            	pubKey = areg.getPubKey();
                
            } catch (IOException | GeneralSecurityException ex) {
                throw new IllegalStateException(ex);
            }
            
            LOG.debug("PubKey Tx: {} => {} => {}", new Object[] { tx.txId(), cid, pubKey });
        }

        return pubKey;
    }

    protected FHandle getFHandleFromTx(Address owner, UTXO utxo) {
        AssertArgument.assertNotNull(utxo, "Null utxo");

        Tx tx = wallet.getTransaction(utxo.getTxId());
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
        if (hashBytes != null && outAddr != null) {

            // Get the file id from stored data
            String cid = new String(hashBytes);

            // Get the file from IPFS
            LOG.debug("File Tx: {} => {}", tx.txId(), cid);
            
            // Not owned by the given address
            if (owner != null && !owner.equals(outAddr)) return null;
            
            fhandle = new FHBuilder(owner, cid)
                    .txId(tx.txId())
                    .owner(owner)
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
    
    private void assertArgumentHasPrivateKey(Address addr) {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);
    }

    private void assertArgumentHasLabel(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().isEmpty(), "Address has no label: " + addr);
    }
    
    private void assertArgumentNotChangeAddress(Address addr) {
        AssertArgument.assertTrue(!addr.getLabels().contains(Wallet.LABEL_CHANGE), "Cannot use change address: " + addr);
    }
    
    private Path assertValidPlainPath(Address owner, Path path) {
        
        AssertArgument.assertNotNull(path, "Null path");
        AssertArgument.assertTrue(!path.isAbsolute(), "Not a relative path: " + path);
        
        String pstr = path.toString();
        AssertArgument.assertTrue(pstr.trim().length() > 0, "Empty path");
        
        return getPlainPath(owner).resolve(path);
    }

    class AsyncGetCallable implements Callable<FHandle> {
        
        final long timeout;
        final FHandle fhandle;
        
        AsyncGetCallable(FHandle fhandle, long timeout) {
            AssertArgument.assertNotNull(fhandle, "Null fhandle");
            AssertArgument.assertTrue(fhandle.isScheduled(), "Not scheduled");
            this.timeout = timeout;
            this.fhandle = fhandle;
        }

        @Override
        public FHandle call() throws Exception {
            
            int attempt = fhandle.getAttempt() + 1;
            
            FHandle fhaux = new FHBuilder(fhandle)
                    .attempt(attempt)
                    .build();
            
            filecache.put(fhaux);
            
            LOG.info("{}: {}", logPrefix("attempt", attempt),  fhaux);
            
            try {
                
                fhaux = ipfsGet(fhaux, timeout);
                
            } catch (Exception ex) {
                
                fhaux = processException(fhaux, ex);
                
            } finally {
                
                fhaux.setScheduled(false);
                filecache.put(fhaux);
            }

            return fhaux;
        }
        
        private FHandle processException(FHandle fhres, Exception ex) throws InterruptedException {
            
            fhres = filecache.get(fhres.getCid());
            int attempt = fhres.getAttempt();
            
            if (ex instanceof IPFSTimeoutException) {
                
                if (config.getIpfsAttempts() <= attempt) {
                    fhres = new FHBuilder(fhres)
                            .expired(true)
                            .build();
                }
                
                LOG.info("{}: {}", logPrefix("timeout", attempt),  fhres);
            }
            
            else if (ex instanceof MerkleNotFoundException) {
                
                fhres = new FHBuilder(fhres)
                        .expired(true)
                        .build();
                
                LOG.warn("{}: {}", logPrefix("no merkle", attempt),  fhres);
            }
            
            else {
                
                fhres = new FHBuilder(fhres)
                        .expired(true)
                        .build();
                
                LOG.error(logPrefix("error", attempt) + ": " + fhres, ex);
            }
            
            return fhres;
        }

        private String logPrefix(String action, int attempt) {
            int ipfsAttempts = config.getIpfsAttempts();
            String trdName = Thread.currentThread().getName();
            return String.format("IPFS %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
        }
    }
    
    static class IPFSFileCache {
        
        private final Map<String, FHandle> filecache = Collections.synchronizedMap(new LinkedHashMap<>());

        Set<String> keySet() {
            return filecache.keySet();
        }
        
        void clear() {
            filecache.clear();
        }

        FHandle get(String cid) {
            return filecache.get(cid);
        }
        
        FHandle put(FHandle fhandle) {
            String cid = fhandle.getCid();
            AssertArgument.assertNotNull(cid, "Null cid");
            LOG.debug("Cache put: {}", fhandle);
            return filecache.put(cid, fhandle);
        }
        
        FHandle remove(String cid) {
            LOG.debug("Cache remove: {}", cid);
            return filecache.remove(cid);
        }
    }
}