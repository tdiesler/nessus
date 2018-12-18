package io.nessus.ipfs.jaxrs;

import io.nessus.cipher.utils.CipherSanityCheck;

public class JAXRSSanityCheck {

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
