package io.nessus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface Block {
    
    String hash();

    int confirmations();

    int size();

    int height();

    int version();

    String merkleRoot();

    List<String> tx();

    Date time();

    long nonce();

    String bits();

    BigDecimal difficulty();

    String previousHash();

    String nextHash();

    String chainwork();

    Block previous();

    Block next();
}