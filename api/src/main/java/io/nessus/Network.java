package io.nessus;

import java.math.BigDecimal;
import java.util.List;

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
    List<String> generate(int numBlocks, String address);
}
