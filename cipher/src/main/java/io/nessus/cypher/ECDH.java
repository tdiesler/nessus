package io.nessus.cypher;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.KeyAgreement;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

public class ECDH {

    private final KeyPairGenerator kpgen;
    
    public ECDH() throws GeneralSecurityException {

        kpgen = KeyPairGenerator.getInstance("EC", "BC");
        kpgen.initialize(new ECGenParameterSpec("prime192v1"), new SecureRandom());
    }

    public KeyPair generateKeyPair() {
        return kpgen.generateKeyPair();
    }
    
    public byte[] savePublicKey(PublicKey key) throws Exception {
        ECPublicKey eckey = (ECPublicKey) key;
        return eckey.getQ().getEncoded(true);
    }

    public byte[] savePrivateKey(PrivateKey key) throws Exception {
        ECPrivateKey eckey = (ECPrivateKey) key;
        return eckey.getD().toByteArray();
    }

    public byte[] generateSecret(byte[] dataPrv, byte[] dataPub) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(loadPrivateKey(dataPrv));
        ka.doPhase(loadPublicKey(dataPub), true);
        return ka.generateSecret();
    }
    
    private PublicKey loadPublicKey(byte[] data) throws Exception {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");
        ECPublicKeySpec pubKey = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        return kf.generatePublic(pubKey);
    }

    private PrivateKey loadPrivateKey(byte[] data) throws Exception {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");
        ECPrivateKeySpec prvkey = new ECPrivateKeySpec(new BigInteger(data), params);
        KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        return kf.generatePrivate(prvkey);
    }
}