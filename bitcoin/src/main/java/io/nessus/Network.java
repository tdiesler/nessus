package io.nessus;

import java.util.List;

public interface Network {

    List<String> mineBlocks(int numBlocks);
    
    List<String> mineBlocks(int numBlocks, String address);
}
