/*
 * Java HTTP Proxy Library (wpg-proxy), more info at
 * http://wpg-proxy.sourceforge.net/
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * 
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.wpg.proxy;

import org.apache.log4j.Logger;
import org.bouncycastle.x509.X509V1CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * The utility class that manages certificates.
 * 
 */
public final class CertificateManager {

  /** Logger object used for all logging activities. */
  private static Logger logger = Logger.getLogger(CertificateManager.class);

  /** The keysore full path. */
  private String keyStore;

  /** Alias of the key in the KeyStore. */
  private String alias;

  /** KeyStore password. */
  private char[] password;

  /** Maps principal to KeyStore. */
  private Map<X500Principal, KeyStore> keyStoreMap =
      new HashMap<X500Principal, KeyStore>();

  /** The SSL Context for the Server. */
  private SSLContext sslContext;

  /** CA Cerificate. */
  private X509Certificate cert;

  /** The private key of the CA. */
  private PrivateKey privateKey;

  /** Constant value - number of milliseconds in a 30-day month. */
  private static final long MS_IN_MONTH = 30 * 24 * 60 * 60 * 1000L;

  /** Constant value - number of milliseconds in a non-leap year. */
  private static final long MS_IN_YEAR = 365 * 24 * 60 * 60 * 1000L;

  /** Algorithm used for the key generation. */
  private static final String KEY_ALGORITHM = "RSA";

  /** The size of the generated key. */
  private static final int KEYSIZE = 1024;

  /** The algorithm for the signature. */
  private static final String SIGNATURE_ALGORITHM = "SHA1WITHRSA";

  /** Secure socket protocol used. */
  private static final String SS_PROTOCOL = "TLS";

  /**
   * Constructs CertificateManager initialized with a given KeyStoreConfig
   * 
   * @param ksc - keyStoreConfig to initialize with
   * @throws IOException thrown if CertficateManger cannot be created
   */
  public CertificateManager(final KeyStoreConfig ksc) throws IOException,
      GeneralSecurityException {
    initCASSLContext(ksc);
    populateCACertificateAndPKFromKeyStore();
  }

  /**
   * Issues a certificate for a given entity. It's validity will be from one
   * month before the current date till one one year after the current date.
   * 
   * @param dn - DN of the entity that requires a certificate
   * @return SSLContext initialized with the issued certificate
   * @throws IOException - thrown if the certificate cannot be issued.
   */
  public SSLContext issueCertificate(final String dn) throws IOException {
    long curTime = System.currentTimeMillis();
    return issueCertificate(new X500Principal(dn), new Date(curTime
        - MS_IN_MONTH), new Date(curTime + MS_IN_YEAR));
  }

  private X509Certificate getCACertificate() {
    return cert;
  }

  /**
   * Populates CA certificate and private key from the KeyStore.
   * 
   * @throws GeneralSecurityException - thrown if Keystore cannot be initialized
   * @throws IOException - thrown if keystore file cannot be read
   */
  private void populateCACertificateAndPKFromKeyStore() throws IOException {
    KeyStore keystore = null;
    FileInputStream fileinputstream = new FileInputStream(keyStore);
    try {
      keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(fileinputstream, password);
      Certificate[] certs = keystore.getCertificateChain(alias);
      privateKey = (PrivateKey) keystore.getKey(alias, password);
      cert = (X509Certificate) certs[0];
    } catch (GeneralSecurityException gse) {
      throw new IOException(gse.getCause());
    }
  }

  /**
   * Issues a certificate for a given Principal.
   * 
   * @param x500principal - the principal to issue the certificate for
   * @param fromDate - certificate validity start date
   * @param toDate - certificate validity end date
   * @return SSLContext initialized with the issued certificate
   * @throws IOException - If the certificate cannot be issued
   */
  public SSLContext issueCertificate(final X500Principal x500principal,
      final Date fromDate, final Date toDate) throws IOException {
    SSLContext sslcontext;
    try {
      KeyStore keystore;
      synchronized (keyStoreMap) {
        keystore = keyStoreMap.get(x500principal);
      }
      if (keystore == null) {
        keystore = createKeystore(x500principal, fromDate, toDate);
        synchronized (keyStoreMap) {
          keyStoreMap.put(x500principal, keystore);
        }
      }
      KeyManagerFactory kmf =
          KeyManagerFactory
              .getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keystore, password);
      TrustManager[] trustmanagers = new TrustManager[1];
      trustmanagers[0] = new WorkAroundX509TrustManager();
      sslcontext = SSLContext.getInstance(SS_PROTOCOL);
      sslcontext.init(kmf.getKeyManagers(), trustmanagers, null);
    } catch (GeneralSecurityException ex) {
      throw new IOException(ex.toString());
    }
    return sslcontext;
  }

  public SSLContext getSSLContext() {
    return sslContext;
  }

  /**
   * Creates a Keystore for a given Principal with given start and end dates.
   * 
   * @param x500principal - the principal to issue the certificate for
   * @param fromDate - certificate validity start date
   * @param toDate - certificate validity end date
   * @return Keystore initialized with the issued certificate
   * @throws IOException - thrown if the keystore cannot be created
   * @throws GeneralSecurityException thrown if the keystore cannot be created
   */
  private KeyStore createKeystore(final X500Principal x500principal,
      final Date fromDate, final Date toDate) throws IOException,
      GeneralSecurityException {
    X509Certificate issuerCertificate = getCACertificate();
    KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
    kpg.initialize(KEYSIZE);
    KeyPair keypair = kpg.generateKeyPair();
    X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
    long curTime = System.currentTimeMillis();
    certGen.setSerialNumber(BigInteger.valueOf(curTime++));
    certGen.setIssuerDN(issuerCertificate.getSubjectX500Principal());
    certGen.setSubjectDN(x500principal);
    certGen.setNotBefore(fromDate);
    certGen.setNotAfter(toDate);
    certGen.setPublicKey(keypair.getPublic());
    certGen.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
    Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
    X509Certificate crt =
        certGen.generate(privateKey, signature.getProvider().getName());
    Certificate[] certChain = new Certificate[2];
    certChain[0] = crt;
    certChain[1] = issuerCertificate;
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, password);
    keystore.setKeyEntry(alias, keypair.getPrivate(), password, certChain);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    keystore.store(baos, password);
    keystore.load(new ByteArrayInputStream(baos.toByteArray()), password);
    return keystore;
  }

  /**
   * Initializes SSLContext using provided keystore.
   * 
   * @param ksc - config for the keystore.
   * @return initialized SSLContext
   * @throws IOException - thrown if the context cannot be initialized
   * @throws GeneralSecurityException
   */
  private SSLContext initCASSLContext(final KeyStoreConfig ksc)
      throws IOException, GeneralSecurityException {
    keyStore = ksc.getKeystoreFileName();
    alias = ksc.getAlias();
    password = ksc.getKeystorePassword().toCharArray();
    TrustManager[] trustmanager = new TrustManager[1];
    trustmanager[0] = new WorkAroundX509TrustManager();
    KeyStore keystore = null;
    FileInputStream fis = new FileInputStream(keyStore);
    keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(fis, password);
    KeyManagerFactory kmf =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keystore, password);
    sslContext = SSLContext.getInstance(SS_PROTOCOL);
    sslContext.init(kmf.getKeyManagers(), trustmanager, new SecureRandom());
    return sslContext;
  }

  /** Dummy Trust manager class. */
  class WorkAroundX509TrustManager implements X509TrustManager {

    /** The certificate chain. */
    private X509Certificate[] c;

    /**
     * Given the partial or complete certificate chain provided by the peer,
     * build a certificate path to a trusted root and return if it can be
     * validated and is trusted for client SSL authentication based on the
     * authentication type.
     * 
     * @param chain - the peer certificate chain
     * @param authType - the authentication type based on the client certificate
     */
    public void checkClientTrusted(final X509Certificate[] chain,
        final String authType) {
      c = chain;
    }

    /**
     * Given the partial or complete certificate chain provided by the peer,
     * build a certificate path to a trusted root and return if it can be
     * validated and is trusted for server SSL authentication based on the
     * authentication type.
     * 
     * @param chain - the peer certificate chain
     * @param authType - the authentication type based on the client certificate
     */
    public void checkServerTrusted(final X509Certificate[] chain,
        final String authType) {
      c = chain;
    }

    /**
     * Return an array of certificate authority certificates which are trusted
     * for authenticating peers.
     * 
     * @return a non-null (possibly empty) array of acceptable CA issuer
     *         certificates.
     */
    public X509Certificate[] getAcceptedIssuers() {
      return c;
    }
  }
}
