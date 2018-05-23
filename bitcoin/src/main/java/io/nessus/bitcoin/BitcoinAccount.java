package io.nessus.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.nessus.Account;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public class BitcoinAccount implements Account {
    
    final String name;
    final String address;
    
    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);
    
    public BitcoinAccount(String name, String address) {
        this.name = name;
        this.address = address;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPrimaryAddress() {
        return address;
    }
    
    @Override
    public String getPrivKey() {
        return client.dumpPrivKey(address);
    }

    @Override
    public List<String> getAddresses() {
        List<String> result = new ArrayList<>();
        String privKey = client.dumpPrivKey(address);
        for (String addr : client.getAddressesByAccount("")) {
            if (privKey.equals(client.dumpPrivKey(addr))) {
                result.add(addr);
            }
        }
        return Collections.unmodifiableList(result);
    }
}