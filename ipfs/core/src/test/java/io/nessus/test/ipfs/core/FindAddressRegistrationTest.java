package io.nessus.test.ipfs.core;

/*-
 * #%L
 * Nessus :: IPFS :: Core
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

import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;

public class FindAddressRegistrationTest extends AbstractWorkflowTest {

    @Test
    public void findAddressRegistration() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }
        
        Assert.assertNotNull(pubKey);
    }
}

/*

- With a fresh wallet

rm -rf ~/.bitcoin/regtest

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
