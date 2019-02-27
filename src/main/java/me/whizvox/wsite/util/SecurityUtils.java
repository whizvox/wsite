package me.whizvox.wsite.util;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class SecurityUtils {

  // FIXME: I really don't know what I'm doing here
  public static void generateKeystore(InputStream certificate, InputStream privateKey, OutputStream keystore, String alias, char[] password) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    ByteArrayOutputStream pemOut = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int read;
    while ((read = certificate.read(buffer)) != -1) {
      pemOut.write(buffer, 0, read);
    }
    while ((read = privateKey.read(buffer)) != -1) {
      pemOut.write(buffer, 0, read);
    }
    ByteArrayInputStream pemIn = new ByteArrayInputStream(pemOut.toByteArray());
    Certificate cert = cf.generateCertificate(pemIn);
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, password);
    ks.setCertificateEntry(alias, cert);
    ks.store(keystore, password);
  }

}
