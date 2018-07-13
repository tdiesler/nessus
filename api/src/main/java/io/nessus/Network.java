package io.nessus;

import java.math.BigDecimal;
import java.util.List;

import io.nessus.Wallet.Address;

public interface Network {

    /**
     * Estimate the current network fee
     */
    BigDecimal estimateFee();
    
    /**
     * Generate the given number of blocks
     */
    List<String> generate(int numBlocks);
    
    /**
     * Generate the given number of blocks to the given address
     */
    List<String> generate(int numBlocks, Address addr);
    
    /**
     * Get the block for the given hash
     */
    Block getBlock(String blockHash);

    /**
     * Get the best block hash
     */
    String getBestBlockHash();
    
    /**
     * Get the best block
     */
    Block getBestBlock();
    
    /**
     * Get the block count
     */
    Integer getBlockCount();
    
    /**
     * Get the block rate in seconds
     */
    Integer getBlockRate();
}
