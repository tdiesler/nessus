package io.nessus.test.bitcoin.dto;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {

    private WalletConfig wallet;

    public static Config parseConfig(String json) throws IOException {
        
        InputStream jsonData = Config.class.getResourceAsStream(json);
        
        ObjectMapper objectMapper = new ObjectMapper();
        
        Config config = objectMapper.readValue(jsonData, Config.class);
        return config;
    }
    
    public WalletConfig getWallet() {
        return wallet;
    }

    void setWallet(WalletConfig wallet) {
        this.wallet = wallet;
    }

    @Override
    public String toString() {
        return wallet.toString();
    }

    public static class WalletConfig {
        
        private List<Address> addresses;
        
        public List<Address> getAddresses() {
            return Collections.unmodifiableList(addresses);
        }

        void setAddresses(List<Address> address) {
            this.addresses = address;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addresses.size(); i++) {
                Address addr = addresses.get(i);
                sb.append(String.format("%02d: %s\n", i, addr));
            }
            return sb.toString();
        }
    }
    
    public static class Address {

        private String privKey;
        private String pubKey;
        private List<String> labels;

        public String getPrivKey() {
            return privKey;
        }

        void setPrivKey(String privKey) {
            this.privKey = privKey;
        }

        public String getPubKey() {
            return pubKey;
        }

        void setPubKey(String pubKey) {
            this.pubKey = pubKey;
        }

        public List<String> getLabels() {
            return Collections.unmodifiableList(labels);
        }

        void setLabels(List<String> labels) {
            this.labels = labels;
        }

        @Override
        public String toString() {
            return String.format("privKey=%s, pubKey=%s, labels=%s", privKey, pubKey, labels);
        }
    }
}
