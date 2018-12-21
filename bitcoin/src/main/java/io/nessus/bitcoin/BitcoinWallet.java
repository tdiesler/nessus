package io.nessus.bitcoin;

import java.util.ArrayList;
import java.util.HashSet;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.nessus.AbstractWallet;
import io.nessus.Wallet;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinWallet extends AbstractWallet implements Wallet {

    public BitcoinWallet(BitcoinBlockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    @Override
    public Address createNewAddress(List<String> labels) {
    	String lstr = concatLabels(labels);
    	String rawAddr = client.getNewAddress(lstr, "legacy");
        AssertState.assertTrue(isP2PKH(rawAddr), "Not a P2PKH address: " + rawAddr);
        return fromRawAddress(rawAddr, labels);
    }

    @Override
    public Address fromRawAddress(String rawAddr, List<String> labels) {
        return new BitcoinAddress(this, rawAddr, labels);
    }

    @Override
    public boolean isP2PKH(String addr) {
        // https://en.bitcoin.it/wiki/List_of_address_prefixes
        return addr.startsWith("1") || addr.startsWith("m") || addr.startsWith("n");
    }

	@Override
    public List<String> getLabels() {
    	
		Set<String> lbset = new HashSet<>();
        getRawLabels().forEach(lb -> {
        	lbset.addAll(splitLabels(lb).stream()
        			.filter(tok -> !LABEL_CHANGE.equals(tok))
            		.collect(Collectors.toList()));
        });
        
        List<String> result = lbset.stream()
        		.sorted().collect(Collectors.toList());
        
		return result;
    }

	@Override
    @SuppressWarnings("unchecked")
    public List<Address> getAddresses() {
    	List<Address> results = new ArrayList<>();
    	List<String> labels = getRawLabels();
		labels.forEach(lb -> {
    		Map<String, Object> addrs = (Map<String, Object>) query("getaddressesbylabel", lb);
    		addrs.keySet().stream()
    			.filter(raw -> isP2PKH(raw))
    			.forEach(raw -> {
	    			List<String> split = splitLabels(lb);
					Address addr = fromRawAddress(raw, split);
					results.add(addr);
    		});
    	});
        return results;
    }

	@SuppressWarnings("unchecked")
	private List<String> getRawLabels() {
		Object labels = query("listlabels");
		return (List<String>) labels;
	}
}
