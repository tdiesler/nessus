package io.nessus.test.bitcoin;

import java.net.URL;
import java.util.List;

/*-
 * #%L
 * Nessus :: Bitcoin
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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Config;
import io.nessus.Config.Address;

public class ConfigParserTest {

    final Logger LOG = LoggerFactory.getLogger(ConfigParserTest.class);

    @Test
    public void testSimple() throws Exception {

        URL configURL = ConfigParserTest.class.getResource("/initial-import.json");
        Config config = Config.parseConfig(configURL);
        LOG.info("{}", config);
        
        List<Address> addrs = config.getWallet().getAddresses();
		Assert.assertTrue(4 <= addrs.size());
    }
}
