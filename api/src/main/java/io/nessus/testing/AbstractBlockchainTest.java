package io.nessus.testing;

import static io.nessus.Wallet.LABEL_CHANGE;

/*-
 * #%L
 * Nessus :: API
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import io.nessus.Config;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;

public abstract class AbstractBlockchainTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public static final String LABEL_BOB = "Bob";
    public static final String LABEL_MARY = "Mary";
    public static final String LABEL_SINK = "";
    
    public static final String ADDRESS_BOB = "n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE";
    public static final String ADDRESS_MARY = "mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL";
    
    protected static Blockchain blockchain;
    protected static Network network;
    protected static Wallet wallet;
    
    protected static Address addrBob;
    protected static Address addrMary;
    protected static Address addrSink;
    
    @Before
    public void before() throws Exception {

    	if (blockchain == null) {
    		
        	blockchain = getBlockchain();
        	network = blockchain.getNetwork();
        	wallet = blockchain.getWallet();

        	// Create the default address on demnad 
        	
        	addrSink = wallet.getAddress(LABEL_SINK);
        	if (addrSink == null) 
        		addrSink = wallet.newAddress(LABEL_SINK);
        	
            // Import the configured addresses and generate a few coins
        	
            importAddresses(getClass());
            
            // Generate initial coins
            
            BigDecimal balance = wallet.getBalance(addrSink);
            if (BigDecimal.ZERO.compareTo(balance) <= 0)
            	generate(101, addrSink);
            
            addrBob = wallet.findAddress(ADDRESS_BOB);
            addrMary = wallet.findAddress(ADDRESS_MARY);
            
            // Unlock all UTXOs
            
            List<Address> addrs = wallet.getAddresses();
            wallet.listLockUnspent(addrs).forEach(utxo -> {
            	wallet.lockUnspent(utxo, true);
            });
            
            // Send everything to the sink
            
            List<UTXO> utxos = wallet.listUnspent(addrs);
            if (!utxos.isEmpty()) {
                String rawSink = addrSink.getAddress();
                wallet.sendToAddress(rawSink, rawSink, Wallet.ALL_FUNDS, utxos);
                generate(1, addrSink);
            }
    	}
    }
    
	protected abstract Blockchain createBlockchain();

	protected Blockchain getBlockchain() {
		if (blockchain == null) {
			blockchain = createBlockchain();
		}
		return blockchain;
	}

    protected void importAddresses(Class<?> configSource) throws IOException {
        
        URL configURL = configSource.getResource("/initial-import.json");
        if (configURL != null) {
            Config config = Config.parseConfig(configURL);
            wallet.importAddresses(config);
        }
    }
    
    protected void generate(int blocks, Address addr) {
        List<String> hashs = network.generate(blocks, addr);
        Assert.assertEquals(blocks, hashs.size());
    }
    
    protected BigDecimal getUTXOAmount(List<UTXO> utxos) {
        return AbstractWallet.getUTXOAmount(utxos);
    }
    
    protected void showAccountBalances() {
    	
    	Map<String, List<Address>> groups = new HashMap<>();
    	wallet.getAddresses().forEach(addr -> {
    		addr.getLabels().stream()
    			.filter(lb -> !LABEL_CHANGE.equals(lb))
    			.forEach(lb -> {
    				List<Address> list = groups.get(lb);
    				if (list == null) {
    					list = new ArrayList<>();
    					groups.put(lb, list);
    				}
    				list.add(addr);
    			});
    	});
    	
        for (String label : groups.keySet().stream().sorted().collect(Collectors.toList())) {
        	
        	Double val = groups.get(label).stream()
        			.mapToDouble(addr -> wallet.getBalance(addr).doubleValue())
        			.sum();
        	
            LOG.info(String.format("%-5s: %13.8f", label, val));
        }
    }
}
