package io.nessus.bitcoin;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Block;
import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet.Address;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinNetwork extends BitcoinClientSupport implements Network {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinNetwork.class);

    static final BigDecimal NETWORK_FEE = new BigDecimal("0.001");
    
    protected BitcoinNetwork(Blockchain blockchain, BitcoindRpcClient client) {
        super(client);
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
    public List<String> generate(int numBlocks, Address address) {
        if (address != null) {
            return client.generateToAddress(numBlocks, address.getAddress());
        } else {
            return client.generate(numBlocks);
        }
    }

    @Override
    public String getBestBlockHash() {
        return client.getBestBlockHash();
    }

    @Override
    public Block getBestBlock() {
        return getBlock(getBestBlockHash());
    }

    @Override
    public Block getBlock(String blockHash) {
        return new BitcoinBlock(client.getBlock(blockHash));
    }

    @Override
    public Integer getBlockCount() {
        return client.getBlockCount();
    }

    @Override
    public Integer getBlockRate() {
        return 600;
    }

}
