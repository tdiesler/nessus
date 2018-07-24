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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.nessus.Wallet.Address;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class BitcoinAddress extends BitcoinClientSupport implements Address {

    private final BitcoinWallet wallet;
    private final String address;
    private final boolean watchOnly;
    private final List<String> addrLabels;

    public BitcoinAddress(BitcoinWallet wallet, String address, List<String> labels) {
        super(wallet.client);
        this.wallet = wallet;
        this.address = address;
        this.watchOnly = getPrivKey() == null;
        this.addrLabels = Collections.unmodifiableList(new ArrayList<String>(labels));
    }

    @Override
    public String getPrivKey() {
        String privKey = null;
        try {
            privKey = client.dumpPrivKey(address);
        } catch (BitcoinRPCException ex) {
            // ignore
        }
        return privKey;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean isWatchOnly() {
        return watchOnly;
    }

    @Override
    public List<String> getLabels() {
        return Collections.unmodifiableList(addrLabels);
    }

    @Override
    public Address setLabels(List<String> labels) {
        return wallet.updateAddress(this, labels);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Address)) return false;
        Address other = (Address) obj;
        return address.equals(other.getAddress());
    }

    @Override
    public String toString() {
        return String.format("[addr=%s, ro=%b, labels=%s]", address, watchOnly, addrLabels);
    }
}
