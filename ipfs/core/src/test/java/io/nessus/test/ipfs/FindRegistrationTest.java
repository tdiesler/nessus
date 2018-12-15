package io.nessus.test.ipfs;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.core.AHandle;
import io.nessus.ipfs.core.AHandleManager;

public class FindRegistrationTest extends AbstractIpfsTest {

    @Test
    public void findAddrReg() throws Exception {

        // Send 1 BTC to Bob
    	
        BigDecimal balBob = wallet.getBalance(addrBob);
        if (balBob.doubleValue() < 1.0)
            wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        AHandle ahandle = cntmgr.findAddressRegistation(addrBob, null);
        if (ahandle == null) {
        	ahandle = cntmgr.registerAddress(addrBob);
            ahandle = cntmgr.findAddressRegistation(addrBob, null);
        }

        Assert.assertNotNull(ahandle.getPubKey());
    }

    @Test
    public void findMissingAddrReg() throws Exception {

        // Create a new address for Lui
        
        Wallet wallet = cntmgr.getBlockchain().getWallet();
        Address addrLui = wallet.newAddress(LABEL_LUI);
        try {
        	
            AHandle ahA = cntmgr.findAddressRegistation(addrLui, null);
            Assert.assertNull(ahA);
            
            // Send 1 BTC to Lui
        	
            BigDecimal balBob = wallet.getBalance(addrLui);
            if (balBob.doubleValue() < 1.0)
                wallet.sendToAddress(addrLui.getAddress(), new BigDecimal("1.0"));
            
            // Register an address without IPFS add
            
            AHandle ahB = cntmgr.registerAddress(addrLui, true);
            Assert.assertNotNull(ahB.getPubKey());
            Assert.assertTrue(ahB.isAvailable());
            Assert.assertNotNull(ahB.getCid());
            
            // Clear the IPFS cache
            
            cntmgr.getIPFSCache().clear();
                        
            AHandle ahC = cntmgr.findAddressRegistation(addrLui, 5000L);
            Assert.assertFalse(ahC.isAvailable());
            
            // Add the IPFS file
            AHandleManager ahmgr = cntmgr.getAHandleManager();
            AHandle ahD = ahmgr.addIpfsContent(ahB, false);
            Assert.assertEquals(ahB.getCid(), ahD.getCid());
            
            // Clear the IPFS cache again
            
            cntmgr.getIPFSCache().clear();
                        
            AHandle ahE = cntmgr.findAddressRegistation(addrLui, null);
            Assert.assertTrue(ahE.isAvailable());
            
        } finally {
        	cntmgr.unregisterAddress(addrLui);
        }
        
    }

}
