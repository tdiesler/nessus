package io.nessus.core.ipfs.impl;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import io.nessus.core.ipfs.ContentManager;
import io.nessus.core.ipfs.FHandle;
import io.nessus.core.ipfs.FHandle.FHBuilder;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.core.ipfs.IPFSException;
import io.nessus.core.ipfs.IPFSTimeoutException;
import io.nessus.core.ipfs.MerkleNotFoundException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class DefaultContentManager implements ContentManager {

    public static final long DEFAULT_IPFS_TIMEOUT = 6000; // 6 sec
    public static final int DEFAULT_IPFS_ATTEMPTS = 100; // 10 min
    public static final int DEFAULT_IPFS_THREADS = 12;

    static final Logger LOG = LoggerFactory.getLogger(DefaultContentManager.class);

    protected final Blockchain blockchain;
    protected final Network network;
    protected final Wallet wallet;

    protected final FHeaderId fhid;
    protected final IPFSClient ipfs;
    protected final BCData bcdata;
    protected final Path rootPath;

    // Executor service for async IPFS get operations
    final ExecutorService executorService;
    
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
    
    private final long ipfsTimeout;
    private final int ipfsAttempts;
    private final int ipfsThreads;
    
    public DefaultContentManager(IPFSClient ipfs, Blockchain blockchain) {
        this(ipfs, blockchain, DEFAULT_IPFS_TIMEOUT, DEFAULT_IPFS_ATTEMPTS, DEFAULT_IPFS_THREADS);
    }
    
    public DefaultContentManager(IPFSClient ipfs, Blockchain blockchain, Long timeout, Integer attepts, Integer threads) {
        this.blockchain = blockchain;
        this.ipfs = ipfs;
        
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        rootPath = Paths.get(System.getProperty("user.home"), ".nessus");
        rootPath.toFile().mkdirs();
        
        fhid = getFHeaderId();
        bcdata = new BCData(fhid);
        
        ipfsTimeout = timeout != null ? timeout : DEFAULT_IPFS_TIMEOUT;
        ipfsAttempts = attepts != null ? attepts : DEFAULT_IPFS_ATTEMPTS;
        ipfsThreads = threads != null ? threads : DEFAULT_IPFS_THREADS;
        
        LOG.info("DefaultContentManager[timeout={}, attempts={}, threads={}]", ipfsTimeout, ipfsAttempts, ipfsThreads);
        
        executorService = Executors.newFixedThreadPool(ipfsThreads, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
    }

    public long getIPFSTimeout() {
        return ipfsTimeout;
    }

    public int getIPFSAttempts() {
        return ipfsAttempts;
    }

    public int getIPFSThreads() {
        return ipfsThreads;
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
    public PublicKey registerAddress(Address addr) throws GeneralSecurityException {
        AssertArgument.assertNotNull(addr, "Null addr");
        
        assertArgumentHasLabel(addr);
        assertArgumentHasPrivateKey(addr);
        assertArgumentNotChangeAddress(addr);

        // Do nothing if already registered
        PublicKey pubKey = findAddressRegistation(addr);
        if (pubKey != null)
            return pubKey;

        // Store the EC key, which is derived from the privKey

        KeyPair keyPair = getECKeyPair(addr);
        pubKey = keyPair.getPublic();
        
        byte[] keyBytes = pubKey.getEncoded();
        byte[] data = bcdata.createPubKeyData(keyBytes);

        // Send a Tx to record the OP_RETURN data

        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = addr.getLabels().get(0);
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

        LOG.info("PubKey register: {} => Tx {}", addr, txId);

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
        
        LOG.info("Unregister PubKey: {} => {} => {}", addr, pubKey, txId);
        
        return pubKey;
    }

    @Override
    public List<String> removeIPFSContent(Address owner, List<String> cids) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        assertArgumentHasLabel(owner);

        List<String> results = new ArrayList<>();
        
        List<UTXO> utxos = wallet.listLockUnspent(Arrays.asList(owner)).stream()
                .filter(utxo -> {
                    FHandle fh = getFHandleFromTx(utxo, owner);
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
    public FHandle add(Address owner, InputStream input, Path path) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(input, "Null input");
        AssertArgument.assertNotNull(path, "Null path");
        
        assertArgumentHasPrivateKey(owner);
        
        PublicKey pubKey = findAddressRegistation(owner);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + owner);
        
        Path plainPath = assertValidPlainPath(owner, path);
        AssertState.assertFalse(plainPath.toFile().exists(), "Local content already exists: " + plainPath);
        
        LOG.info("Start IPFS Add: {} {}", owner, path);
        
        plainPath.getParent().toFile().mkdirs();
        Files.copy(input, plainPath);
        
        URL furl = plainPath.toFile().toURI().toURL();
        FHandle fhandle = new FHBuilder(owner, path, furl).build();

        LOG.info("IPFS encrypt: {}", fhandle);
        
        fhandle = encrypt(fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle);
        
        Path auxPath = Paths.get(fhandle.getURL().getPath());
        String cid = ipfs.addSingle(auxPath);
        
        // Move the temp file to its crypt path
        
        Path cryptPath = getCryptPath(owner).resolve(cid);
        Path tmpPath = Paths.get(fhandle.getURL().getPath());
        Files.move(tmpPath, cryptPath);
        
        furl = cryptPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("IPFS record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, owner, owner);
        
        fhandle = new FHBuilder(fhandle)
                .available(true)
                .build();
        
        filecache.put(fhandle);
        
        LOG.info("Done IPFS Add: {}", fhandle);
        
        return fhandle;
    }

    @Override
    public FHandle get(Address owner, String cid, Path path, Long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(cid, "Null cid");
        AssertArgument.assertNotNull(path, "Null path");

        assertArgumentHasPrivateKey(owner);

        LOG.info("Start IPFS Get: {} {}", owner, path);
        
        timeout = timeout != null ? timeout : ipfsTimeout;
        FHandle fhandle = ipfsGet(cid, timeout);
        
        LOG.info("IPFS decrypt: {}", fhandle);
        
        fhandle = decrypt(fhandle, owner, path, true);
        
        LOG.info("Done IPFS Get: {}", fhandle);
        
        return fhandle;
    }
    
    @Override
    public FHandle send(Address owner, String cid, Address target, Long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(cid, "Null cid");
        AssertArgument.assertNotNull(target, "Null target");
        
        assertArgumentHasLabel(owner);
        assertArgumentHasPrivateKey(owner);
        assertArgumentNotChangeAddress(owner);
        
        PublicKey pubKey = findAddressRegistation(target);
        AssertArgument.assertTrue(pubKey != null, "Cannot obtain encryption key for: " + target);
        
        LOG.info("Start IPFS Send: {} {}", owner, cid);
        
        timeout = timeout != null ? timeout : ipfsTimeout;
        FHandle fhandle = ipfsGet(cid, timeout);
        
        LOG.info("IPFS decrypt: {}", fhandle);

        fhandle = decrypt(fhandle, owner, null, false);

        fhandle = new FHBuilder(fhandle)
                .owner(target)
                .cid(null)
                .build();

        LOG.info("IPFS encrypt: {}", fhandle);
        
        fhandle = encrypt(fhandle, pubKey);
        
        LOG.info("IPFS add: {}", fhandle);
        
        Path tmpPath = Paths.get(fhandle.getURL().getPath());
        cid = ipfs.addSingle(tmpPath);
        
        Path cryptPath = getCryptPath(target).resolve(cid);
        Files.move(tmpPath, cryptPath);

        URL furl = cryptPath.toUri().toURL();
        fhandle = new FHBuilder(fhandle)
                .url(furl)
                .cid(cid)
                .build();
        
        LOG.info("IPFS record: {}", fhandle);
        
        fhandle = recordFileData(fhandle, owner, target);
        
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
    public List<FHandle> findIPFSContent(Address owner, Long timeout) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");

        // The list of files that are recorded and unspent
        List<FHandle> unspentFHandles = new ArrayList<>();
        
        synchronized (filecache) {
        
            List<UTXO> locked = listLockedAndUnlockedUnspent(owner, true, false);
            List<UTXO> unspent = listLockedAndUnlockedUnspent(owner, true, true);
            
            for (UTXO utxo : unspent) {
                
                String txId = utxo.getTxId();
                Tx tx = wallet.getTransaction(txId);
                
                FHandle fhandle = getFHandleFromTx(utxo, owner);
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
        timeout = timeout != null ? timeout : ipfsTimeout;
        List<FHandle> results = ipfsGetAsync(unspentFHandles, timeout);
        
        return results;
    }

    @Override
    public List<FHandle> findLocalContent(Address owner) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        Path fullPath = getPlainPath(owner);
        return findLocalContent(owner, fullPath, new ArrayList<>());
    }

    @Override
    public FHandle findLocalContent(Address owner, Path path) throws IOException {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(path, "Null path");
        
        Path fullPath = getPlainPath(owner).resolve(path);
        URL furl = fullPath.toUri().toURL();
        
        FHandle fhandle = new FHBuilder(owner, path, furl)
                .available(fullPath.toFile().exists())
                .build();
        
        return fhandle;
    }

    private List<FHandle> findLocalContent(Address owner, Path fullPath, List<FHandle> fhandles) throws IOException {
        
        if (fullPath.toFile().isDirectory()) {
            for (String child : fullPath.toFile().list()) {
                findLocalContent(owner, fullPath.resolve(child), fhandles);
            }
        }
        
        if (fullPath.toFile().isFile()) {
            
            Path relPath = getPlainPath(owner).relativize(fullPath);
            FHandle fhandle = findLocalContent(owner, relPath);
            fhandles.add(fhandle);
        }
        
        return fhandles;
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
        if (!plainPath.toFile().exists()) return true;
        
        return removeLocalContentInternal(owner, path);
    }

    private boolean removeLocalContentInternal(Address owner, Path path) throws IOException {
        
        Path plainPath = assertValidPlainPath(owner, path);
        Files.walkFileTree(plainPath, new SimpleFileVisitor<Path>() {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                String[] list = dir.toFile().list();
                if (list != null && list.length == 0) { 
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        Path relParent = path.getParent();
        if (relParent != null) {
            Path plainParent = assertValidPlainPath(owner, relParent);
            String[] list = plainParent.toFile().list();
            if (list != null && list.length == 0) { 
                removeLocalContentInternal(owner, relParent);
            }
        }
        
        return !path.toFile().exists();
    }

    protected BitcoindRpcClient getRpcClient() {
        return ((RpcClientSupport) blockchain).getRpcClient();
    }

    protected FHeaderId getFHeaderId() {
        return new FHeaderId("Nessus", "1.0");
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
    
    private FHandle ipfsGet(String cid, long timeout) throws IOException, IPFSTimeoutException {
        
        FHandle fhandle;
        synchronized (filecache) {
            
            fhandle = filecache.get(cid);
            if (fhandle != null && !fhandle.isMissing())
                return fhandle;
            
            if (fhandle == null) {
                fhandle = new FHBuilder(cid).build();
                filecache.put(fhandle);
            }
        }

        long before = System.currentTimeMillis();
        
        Path tmpPath;
        try {
            
            Future<Path> future = ipfs.get(cid, getTempPath());
            tmpPath = future.get(timeout, TimeUnit.MILLISECONDS);
            
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
            
        AssertState.assertTrue(tmpPath.toFile().exists(), "Cannot obtain file from: " + tmpPath);

        long elapsed = System.currentTimeMillis() - before;
        
        fhandle = new FHBuilder(fhandle)
                .url(tmpPath.toUri().toURL())
                .elapsed(elapsed)
                .available(true)
                .build();

        try (FileReader fr = new FileReader(tmpPath.toFile())) {
            BufferedReader br = new BufferedReader(fr);

            FHeader header = readFHeader(br);
            Address addr = getAddress(header.owner);
            AssertState.assertNotNull(addr, "Address unknown to this wallet: " + header.owner);

            LOG.debug("IPFS token: {} => {}", cid, header.token);

            fhandle = new FHBuilder(fhandle)
                    .owner(getAddress(header.owner))
                    .secretToken(header.token)
                    .path(header.path)
                    .build();
        }
        
        // [TODO] Instead of replace, this file op can likely be optimized
        Path cryptPath = getCryptPath(fhandle.getOwner()).resolve(cid);
        Files.move(tmpPath, cryptPath, StandardCopyOption.REPLACE_EXISTING);

        fhandle = new FHBuilder(fhandle)
                .url(cryptPath.toUri().toURL())
                .build();
        
        LOG.info("IPFS found: {}", fhandle);

        filecache.put(fhandle);
        
        return fhandle;
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
    
    private FHandle recordFileData(FHandle fhandle, Address fromAddr, Address toAddr) throws GeneralSecurityException {

        AssertArgument.assertTrue(fhandle.isEncrypted(), "File not encrypted: " + fhandle);

        // Construct the OP_RETURN data

        byte[] data = bcdata.createFileData(fhandle);

        // Send a Tx to record the OP_TOKEN data

        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal spendAmount = dataAmount.add(network.getMinDataAmount());

        String label = fromAddr.getLabels().get(0);
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
    
    protected FHandle decrypt(FHandle fhandle, Address owner, Path destPath, boolean storePlain) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");
        AssertArgument.assertNotNull(owner, "Null owner");

        Path cryptPath = getCryptPath(fhandle.getOwner()).resolve(fhandle.getCid());
        AssertState.assertTrue(cryptPath.toFile().exists(), "Cannot obtain file: " + cryptPath);

        destPath = destPath != null ? destPath : fhandle.getPath();
        AssertArgument.assertTrue(!destPath.isAbsolute(), "Given path must be relative: " + destPath);
        
        // Read the file header
        
        FHeader header;
        try (FileReader fr = new FileReader(cryptPath.toFile())) {
            header = readFHeader(fr);
            AssertState.assertEquals(fhid.VERSION_STRING, header.version);
        }
        
        FHandle fhres;
        
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
            
            // [TODO] How is it possible that the tmp file already exists?
            Path tmpFile = createTempFile();
            Files.copy(decrypted, tmpFile, StandardCopyOption.REPLACE_EXISTING); 
            
            fhres = new FHBuilder(fhandle)
                    .url(tmpFile.toUri().toURL())
                    .secretToken(null)
                    .path(destPath)
                    .build();
        }
        
        if (storePlain) {
            
            Path plainPath = assertValidPlainPath(owner, destPath);
            AssertState.assertFalse(plainPath.toFile().exists(), "Local content already exists: " + plainPath);
            
            plainPath.getParent().toFile().mkdirs();
            
            Path tmpPath = Paths.get(fhres.getURL().getPath());
            Files.move(tmpPath, plainPath);
            
            URL furl = plainPath.toFile().toURI().toURL();
            fhres = new FHBuilder(owner, destPath, furl)
                    .available(true)
                    .build();
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

        // Expect OP_PUB_KEY
        byte[] txdata = out1.getData();
        byte[] keyBytes = bcdata.extractPubKeyData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (keyBytes != null && outAddr != null) {

            // Not owned by the given address
            if (owner != null && !owner.equals(outAddr)) return null;
            
            pubKey = new PublicECKey(keyBytes); 
            LOG.debug("PubKey Tx: {} => {} => {}", new Object[] { tx.txId(), outAddr, pubKey });
        }

        return pubKey;
    }

    protected FHandle getFHandleFromTx(UTXO utxo, Address owner) {
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
            
            fhandle = new FHBuilder(cid)
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

    protected FHeader readFHeader(Reader rd) throws IOException {
        return FHeader.create(fhid, rd);
    }

    public FHeader writeHeader(FHandle fhandle, Writer pw) throws GeneralSecurityException, IOException {
        
        String owner = fhandle.getOwner().getAddress();
        String base64Token = fhandle.getSecretToken();
        
        LOG.debug("IPFS token: {}", base64Token);
        
        FHeader header = new FHeader(fhid.VERSION_STRING, fhandle.getPath(), owner, base64Token, -1);
        header.write(fhid, pw);
        
        return header;
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
            
            String cid = fhandle.getCid();
            int attempt = fhandle.getAttempt() + 1;
            
            FHandle fhres = new FHBuilder(fhandle)
                    .attempt(attempt)
                    .build();
            
            filecache.put(fhres);
            
            LOG.info("{}: {}", logPrefix("attempt", attempt),  fhres);
            
            try {
                
                fhres = ipfsGet(cid, timeout);
                
            } catch (IPFSException ex) {
                
                fhres = processIPFSException(fhres, ex);
                
            } finally {
                
                fhres.setScheduled(false);
                filecache.put(fhres);
            }

            return fhres;
        }
        
        private FHandle processIPFSException(FHandle fhres, IPFSException ex) throws InterruptedException {
            
            fhres = filecache.get(fhres.getCid());
            int attempt = fhres.getAttempt();
            
            if (ex instanceof IPFSTimeoutException) {
                
                if (ipfsAttempts <= attempt) {
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
                
                LOG.error("{}: {}", logPrefix("error", attempt),  fhres);
            }
            
            return fhres;
        }

        private String logPrefix(String action, int attempt) {
            String trdName = Thread.currentThread().getName();
            return String.format("IPFS %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
        }
    }
    
    @SuppressWarnings("serial")
    static class PublicECKey implements PublicKey {
        
        final byte[] keyBytes;
        final String encKey;

        PublicECKey(byte[] keyBytes) {
            this.keyBytes = keyBytes;
            this.encKey = Base64.getEncoder().encodeToString(keyBytes);
            
        }

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

        @Override
        public int hashCode() {
            return Arrays.hashCode(keyBytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PublicECKey)) return false;
            PublicECKey other = (PublicECKey) obj;
            return Arrays.equals(keyBytes, other.keyBytes);
        }
        
        public String toString() {
            return encKey;
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
    
    public static class FHeaderId {
        
        public final String PREFIX;
        public final String VERSION;
        public final String VERSION_STRING;
        public final String FILE_HEADER_END;
        
        public FHeaderId(String prefix, String version) {
            this.PREFIX = prefix;
            this.VERSION = version;
            this.VERSION_STRING = PREFIX + "-Version: " + VERSION;
            this.FILE_HEADER_END = PREFIX.toUpperCase() + "_HEADER_END";
        }
    }
    
    public static class FHeader {
        
        public final String version;
        public final Path path;
        public final String owner;
        public final String token;
        public final int length;
        
        FHeader(String version, Path path, String owner, String token, int length) {
            this.version = version;
            this.path = path;
            this.owner = owner;
            this.token = token;
            this.length = length;
        }

        static FHeader create(FHeaderId fhid, Reader rd) throws IOException {
            BufferedReader br = new BufferedReader(rd);
            
            // First line is the version
            String line = br.readLine();
            AssertState.assertTrue(line.startsWith(fhid.VERSION_STRING), "Invalid version: " + line);
            
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
                    } else if (line.startsWith(fhid.FILE_HEADER_END)) {
                        line = null;
                    }
                }
            }
            
            return new FHeader(version, path, owner, token, length);
        }

        void write(FHeaderId fhid, Writer wr) {
            PrintWriter pw = new PrintWriter(wr);
            
            // First line is the version
            pw.println(fhid.VERSION_STRING);
            
            // Second is the location
            pw.println(String.format("Path: %s", path));
            
            // Then comes the owner
            pw.println(String.format("Owner: %s", owner));
            
            // Then comes the encryption token in Base64
            pw.println(String.format("Token: %s", token));
            
            // Then comes an end of header marker
            pw.println(fhid.FILE_HEADER_END);
        }
        
        public String toString() {
            return String.format("[ver=%s, own=%s, path=%s, token=%s]", version, owner, path, token);
        }
    }
    
    static class BCData {
        
        static final byte OP_PUB_KEY = 0x10;
        static final byte OP_FILE_DATA = 0x20;
        static final byte OP_RETURN = 0x6A; 
        
        final String OP_PREFIX;
        
        BCData(FHeaderId fhid) {
            OP_PREFIX = fhid.PREFIX;
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
