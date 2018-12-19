package io.nessus.ipfs.jaxrs;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;

public class SAHandle {

    private String label;
    private String addr;
    private BigDecimal balance;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private String encKey;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean wonly;

    public SAHandle() {
    }

    public SAHandle(Address addr, String encKey, BigDecimal balance) {
        AssertArgument.assertNotNull(addr, "Null addr");
        AssertArgument.assertNotNull(balance, "Null balance");
        AssertArgument.assertTrue(addr.getLabels().size() < 2, "Multiple labels: " + addr);
        this.label = addr.getLabels().stream().findFirst().orElse(null);
        this.addr = addr.getAddress();
        this.encKey = encKey;
        this.balance = balance;
        this.wonly = addr.isWatchOnly();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAddress() {
        return addr;
    }

    public void setAddress(String addr) {
        this.addr = addr;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String encKey) {
        this.encKey = encKey;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isWatchOnly() {
        return wonly;
    }

    public void setWatchOnly(boolean wonly) {
        this.wonly = wonly;
    }

    @JsonIgnore
    public boolean isRegistered() {
        return encKey != null;
    }
    
    public String toString() {
    	String key = encKey != null ? encKey.substring(0, 8) : null;
        return String.format("[addr=%s, label=%s, wo=%b, key=%s, bal=%.6f]",
                addr, label, wonly, key, balance);
    }
}
