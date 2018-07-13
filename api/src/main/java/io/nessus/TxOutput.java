package io.nessus;

import java.math.BigDecimal;

import wf.bitcoin.krotjson.HexCoder;

public class TxOutput {
    
    private final String addr;
    private final BigDecimal amount;
    private final byte[] data;
    
    private String type;
    
    public TxOutput(String addr, BigDecimal amount) {
        this(addr, amount, null);
    }

    public TxOutput(String addr, BigDecimal amount, byte[] data) {
        this.addr = addr;
        this.amount = amount;
        this.data = data;
    }

    public String getAddress() {
        return addr;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String toString() {
        String hex = data != null ? HexCoder.encode(data) : null;
        return String.format("[addr=%s, amnt=%f], data=%s]", addr, amount, hex);
    }
}