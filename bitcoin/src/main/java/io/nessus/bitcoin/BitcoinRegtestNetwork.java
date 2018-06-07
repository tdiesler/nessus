package io.nessus.bitcoin;

import static io.nessus.bitcoin.BitcoinWallet.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;

public class BitcoinRegtestNetwork implements Network {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinRegtestNetwork.class);

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
