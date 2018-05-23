package io.nessus.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinRegtestNetwork implements Network {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinRegtestNetwork.class);

    private final BitcoindRpcClient client;

    public BitcoinRegtestNetwork() {
        this(new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL));
    }

    public BitcoinRegtestNetwork(BitcoindRpcClient client) {
        this.client = client;
    }

    @Override
    public List<String> mineBlocks(int numBlocks) {
        return mineBlocks(numBlocks, null);
    }

    @Override
    public List<String> mineBlocks(int numBlocks, String address) {
        if (address != null) {
            return client.generateToAddress(numBlocks, address);
        } else {
            return client.generate(numBlocks);
        }
    }
}
