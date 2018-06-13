package io.nessus.bitcoin;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinNetwork implements Network {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinNetwork.class);

    static final BigDecimal NETWORK_FEE = new BigDecimal("0.001");
    
    private final BitcoindRpcClient client;
    
    BitcoinNetwork(Blockchain blockchain, BitcoindRpcClient client) {
        this.client = client;
    }

    @Override
    public BigDecimal estimateFee() {
        return NETWORK_FEE;
    }

    @Override
    public List<String> generate(int numBlocks) {
        return generate(numBlocks, null);
    }

    @Override
    public List<String> generate(int numBlocks, String address) {
        if (address != null) {
            return client.generateToAddress(numBlocks, address);
        } else {
            return client.generate(numBlocks);
        }
    }
}
