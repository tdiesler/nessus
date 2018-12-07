package io.nessus.ipfs.jaxrs;

/*-
 * #%L
 * Nessus :: IPFS :: JAXRS
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

import io.nessus.cipher.CipherSanityCheck;

public class JaxrsSanityCheck {

    public static void main(String[] args) throws Exception {

        verifyPlatform();
    }

    public static void verifyPlatform() throws Exception {

        verifySLF4J("slf4jA");

        verifyJBossLogging("jblogA");

        CipherSanityCheck.verifyPlatform();
    }

    public static void verifySLF4J(String name) {
        org.slf4j.Logger slf4jA = org.slf4j.LoggerFactory.getLogger(name);
        slf4jA.debug("SLF4J debug - {}", slf4jA.getName());
    }

    public static void verifyJBossLogging(String name) {
        org.jboss.logging.Logger jblog = org.jboss.logging.Logger.getLogger(name);
        jblog.debugf("JBLog debug - %s", jblog.getName());
    }
}
