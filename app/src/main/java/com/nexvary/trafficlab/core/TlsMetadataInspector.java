package com.nexvary.trafficlab.core;

import java.io.*; import java.security.cert.*; import java.security.MessageDigest; import java.util.*;

public class TlsMetadataInspector {
    public record Metadata(String subject,String issuer,String serial,Date notBefore,Date notAfter,String sigAlg,String sha256){}
    public static Metadata inspectCertificate(byte[] der) throws Exception { CertificateFactory cf=CertificateFactory.getInstance("X.509"); X509Certificate c=(X509Certificate)cf.generateCertificate(new ByteArrayInputStream(der)); return new Metadata(c.getSubjectX500Principal().getName(),c.getIssuerX500Principal().getName(),c.getSerialNumber().toString(16),c.getNotBefore(),c.getNotAfter(),c.getSigAlgName(),hex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded()))); }
    private static String hex(byte[] a){StringBuilder b=new StringBuilder();for(byte x:a)b.append(String.format("%02x",x));return b.toString();}
}
