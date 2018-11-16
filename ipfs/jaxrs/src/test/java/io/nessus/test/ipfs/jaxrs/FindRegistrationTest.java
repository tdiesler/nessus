package io.nessus.test.ipfs.jaxrs;

import java.math.BigDecimal;

/*-
 * #%L
 * Nessus :: IPFS :: JAXRS
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

public class FindRegistrationTest extends AbstractJAXRSTest {

    @Test
    public void findRegistration() throws Exception {

        // Send 1 BTC to Bob
        BigDecimal balBob = wallet.getBalance(addrBob);
        if (balBob.doubleValue() < 1.0)
            wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        PublicKey pubKey = cntmgr.findRegistation(addrBob);

        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }

        Assert.assertNotNull(pubKey);
    }
}
