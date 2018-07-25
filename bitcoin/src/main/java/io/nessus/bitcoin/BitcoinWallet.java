package io.nessus.bitcoin;

import java.util.List;

import io.nessus.AbstractWallet;
import io.nessus.Wallet;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinWallet extends AbstractWallet implements Wallet {

    public BitcoinWallet(BitcoinBlockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    @Override
    protected Address createNewAddress(List<String> labels) {
        String rawAddr = client.getNewAddress(concatLabels(labels), "legacy");
        AssertState.assertTrue(isP2PKH(rawAddr), "Not a P2PKH address: " + rawAddr);
        return createAdddressFromRaw(rawAddr, labels);
    }

    @Override
    protected Address createAdddressFromRaw(String rawAddr, List<String> labels) {
        return new BitcoinAddress(this, rawAddr, labels);
    }

    @Override
    // https://en.bitcoin.it/wiki/List_of_address_prefixes
    protected boolean isP2PKH(String addr) {
        return addr.startsWith("1") || addr.startsWith("m") || addr.startsWith("n");
    }

}
