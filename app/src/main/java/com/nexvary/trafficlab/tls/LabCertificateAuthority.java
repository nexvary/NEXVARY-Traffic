package com.nexvary.trafficlab.tls;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

/** Device-local CA. The signing key is non-exportable in AndroidKeyStore. */
public final class LabCertificateAuthority {
    private static final String ALIAS = "nexvary.traffic.lab.ca.v1";
    private final PrivateKey signingKey;
    private final X509Certificate certificate;
    private final Map<String, SSLContext> contexts = new LinkedHashMap<>();
    private LabCertificateAuthority(PrivateKey key, X509Certificate certificate) {
        this.signingKey = key; this.certificate = certificate;
    }
    public static synchronized LabCertificateAuthority open(Context context) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSubject(new X500Principal("CN=NEXVARY Training CA"))
                    .setCertificateSerialNumber(BigInteger.ONE).build());
            generator.generateKeyPair();
        }
        PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
        File path = new File(context.getFilesDir(), "nexvary-lab-ca.der");
        X509Certificate ca;
        if (path.exists()) {
            try (InputStream in = new FileInputStream(path)) {
                ca = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
            ca.verify(store.getCertificate(ALIAS).getPublicKey());
            ca.checkValidity();
        } else {
            PublicKey publicKey = store.getCertificate(ALIAS).getPublicKey();
            X500Name name = new X500Name("CN=NEXVARY Training CA,O=NEXVARY Lab");
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, serial(),
                    new Date(System.currentTimeMillis() - 300000), new Date(System.currentTimeMillis() + 365L*86400000), name, publicKey);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            ca = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(key)));
            File temporary = new File(context.getFilesDir(), "nexvary-lab-ca.tmp");
            try (FileOutputStream out = new FileOutputStream(temporary)) { out.write(ca.getEncoded()); out.getFD().sync(); }
            if (!temporary.renameTo(path)) throw new IOException("Cannot persist lab certificate");
        }
        return new LabCertificateAuthority(key, ca);
    }
    private static BigInteger serial() { return new BigInteger(128, new SecureRandom()).setBit(127); }
    public byte[] encoded() throws CertificateEncodingException { return certificate.getEncoded(); }
    public X509Certificate certificate() { return certificate; }
    public synchronized SSLContext serverContext(String host) throws Exception {
        if (contexts.containsKey(host)) return contexts.get(host);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        X500Name issuer = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial(),
                new Date(System.currentTimeMillis()-300000), new Date(System.currentTimeMillis()+86400000),
                new X500Name("CN=NEXVARY Lab Endpoint"), pair.getPublic());
        int type = host.matches("[0-9.]+") || host.contains(":") ? GeneralName.iPAddress : GeneralName.dNSName;
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(type, host)));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        X509Certificate leaf = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(signingKey)));
        KeyStore keys = KeyStore.getInstance(KeyStore.getDefaultType()); keys.load(null);
        char[] password = new char[0];
        keys.setKeyEntry("endpoint", pair.getPrivate(), password, new java.security.cert.Certificate[]{leaf, certificate});
        KeyManagerFactory manager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); manager.init(keys, password);
        SSLContext ssl = SSLContext.getInstance("TLS"); ssl.init(manager.getKeyManagers(), null, null);
        if (contexts.size() >= 8) contexts.remove(contexts.keySet().iterator().next());
        contexts.put(host, ssl); return ssl;
    }
}
