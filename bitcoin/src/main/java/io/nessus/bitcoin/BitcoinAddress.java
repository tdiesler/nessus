package io.nessus.bitcoin;

import java.util.List;

import io.nessus.AbstractAddress;
import io.nessus.AbstractWallet;
import io.nessus.Wallet.Address;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinAddress extends AbstractAddress implements Address {

    private final AbstractWallet wallet;

    public BitcoinAddress(AbstractWallet wallet, String address, List<String> labels) {
        super(address, labels);
        this.wallet = wallet;
    }

    @Override
    public String getPrivKey() {
        String privKey = null;
        try {
        	BitcoindRpcClient client = wallet.getRpcClient();
            privKey = client.dumpPrivKey(getAddress());
        } catch (BitcoinRPCException ex) {
            // ignore
        }
        return privKey;
    }

    @Override
    public Address setLabels(List<String> labels) {
        return wallet.updateAddress(this, labels);
    }
}
