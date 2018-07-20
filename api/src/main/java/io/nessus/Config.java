package io.nessus;

/*-
 * #%L
 * Nessus :: API
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {

    private WalletConfig wallet;

    public static Config parseConfig(String json) throws IOException {
        
        Config config = null;
        
        InputStream jsonData = Config.class.getResourceAsStream(json);
        if (jsonData != null) {
            
            ObjectMapper objectMapper = new ObjectMapper();
            config = objectMapper.readValue(jsonData, Config.class);
        } 
        
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
