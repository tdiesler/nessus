package io.nessus.bitcoin;

/*-
 * #%L
 * Nessus :: Bitcoin
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

import java.util.List;

import io.nessus.AbstractWallet;
import io.nessus.Wallet;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinWallet extends AbstractWallet implements Wallet {

    public BitcoinWallet(BitcoinBlockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    @Override
    protected Address createNewAddress(List<String> labels) {
        String rawAddr = client.getNewAddress(concatLabels(labels), "legacy");
        AssertState.assertTrue(isP2PKH(rawAddr), "Not a P2PKH address: " + rawAddr);
        return createAdddressFromRaw(rawAddr, labels);
    }

    @Override
    protected Address createAdddressFromRaw(String rawAddr, List<String> labels) {
        return new BitcoinAddress(this, rawAddr, labels);
    }

    @Override
    public boolean isP2PKH(String addr) {
        // https://en.bitcoin.it/wiki/List_of_address_prefixes
        return addr.startsWith("1") || addr.startsWith("m") || addr.startsWith("n");
    }

}
