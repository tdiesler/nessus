package io.nessus.test.ipfs;

import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;

public class FindRegistrationTest extends AbstractWorkflowTest {

    @Override
    boolean unlockUnspentAfterTest() {
        // set to false when simulating wallet restart
        // rm -rf ~/.bitcoin/regtest
        return true;
    }
    
    @Test
    public void findRegistration() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }
        
        Assert.assertNotNull(pubKey);
    }
}

/*

- First Run

PubKey register: [addr=n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE, ro=false, labels=[Bob]] => Tx fc9cf7c6406239fc45964859430c13776c14cbec34c2f305aae15b1959f28687
Lock unspent: fc9cf7c6406239fc45964859430c13776c14cbec34c2f305aae15b1959f28687 1
PubKey Tx: fc9cf7c6406239fc45964859430c13776c14cbec34c2f305aae15b1959f28687 => [addr=n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE, ro=false, labels=[Bob]] => MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEmrmigcXiUFTpId22iIFfHeq+hl6zJ47S5bIWxVpLpGs=

- Close and reopen the wallet 
- Verify that registration utxo in unlocked (due to wallet restart)
- Rerun the test  

PubKey Tx: fc9cf7c6406239fc45964859430c13776c14cbec34c2f305aae15b1959f28687 => [addr=n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE, ro=false, labels=[Bob]] => MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEmrmigcXiUFTpId22iIFfHeq+hl6zJ47S5bIWxVpLpGs=
Lock unspent: fc9cf7c6406239fc45964859430c13776c14cbec34c2f305aae15b1959f28687 1

*/
