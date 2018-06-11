package io.nessus.bitcoin;

import static io.nessus.bitcoin.BitcoinWallet.client;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;

public class BitcoinNetwork implements Network {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinNetwork.class);

    static final BigDecimal NETWORK_FEE = new BigDecimal("0.001");
    
    @SuppressWarnings("unused")
    private final Blockchain blockchain;
    
    BitcoinNetwork(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public BigDecimal estimateFee() {
        return NETWORK_FEE;
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
