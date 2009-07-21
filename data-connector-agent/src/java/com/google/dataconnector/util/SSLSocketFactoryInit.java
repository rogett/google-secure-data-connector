/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.google.dataconnector.util;

import com.google.dataconnector.client.testing.TrustAllTrustManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * A helper class to set up own local SSL context.
 * 
 * @author vnori@google.com (Vasu Nori)
 */
@Singleton
public class SSLSocketFactoryInit {
  private static final Logger LOG = Logger.getLogger(SSLSocketFactoryInit.class);
  
  /** injected dependencies */
  private final FileUtil fileUtil;
  
  @Inject
  public SSLSocketFactoryInit(FileUtil fileUtil) {
    this.fileUtil = fileUtil;
  }
  
  /**
   * sets up our own local SSL context and returns a SSLSocketFactory 
   * with keystore and password set by our flags.
   * 
   * <p>A keystore contains the list of CAs that the agent accepts. If nothing
   * is specified, it will use the default Java keystore, which contains most
   * well-known CAs - these will be good for Google Tunnel Server.
   * 
   * <p>
   * 
   * @param localConf the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  public SSLSocketFactory getSslSocketFactory(LocalConf localConf) {
    LOG.info("Using SSL for client connections.");
    
    // The following two are required ONLY if the certificate of the server
    // does not map to the default Java CAs. In practice, this will only happen, most
    // likely, when connecting with testing/staging servers with test certificates.
    
    String keystorePath = localConf.getSslKeyStoreFile();
    char[] password = null;
    if (keystorePath != null) {
      // if KeyStoreFile is specified, we know that password must be specified too.
      // {@link LocalConfValidator} makes sure of that.
      password = localConf.getSslKeyStorePassword().toCharArray();
    }
    
    try {
      SSLContext context = SSLContext.getInstance("TLSv1");
      if (keystorePath != null) { // The customer specified their own keystore.
        initializeSslEngineWithCustomKeystore(localConf, password, keystorePath, context);
      } else {
        initializeSslEngineWithDefaultKeystore(context);
      }
      if (context.getSocketFactory() == null) {
        throw new GeneralSecurityException("socketFactory not created");
      }
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      LOG.fatal("SSL setup error", e);
    } catch (IOException e) {
      LOG.fatal("Error reading Keystore file", e);
    }
    return null;
  }

  /**
   * Use the JVM default as trusted store. This would be located somewhere around
   * jdk.../jre/lib/security/cacerts, and will contain widely used CAs.
   */
  private void initializeSslEngineWithDefaultKeystore(SSLContext context)
      throws KeyManagementException {
    context.init(null, null, null);
  }

  /**
   * use the customer suplied keystore.
   */
  private void initializeSslEngineWithCustomKeystore(LocalConf localConf, char[] password,
      String keystorePath, SSLContext context) throws KeyStoreException, IOException,
      NoSuchAlgorithmException, CertificateException, FileNotFoundException, 
      KeyManagementException {
    // Load with our trusted certs and setup the trust manager.
    if (!localConf.getAllowUnverifiedCertificates()) {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(fileUtil.getFileInputStream(keystorePath), password);
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
      tmf.init(keyStore);
      context.init(null, tmf.getTrustManagers(), null);
    } else {
      // Use bogus trust all manager
      context.init(null, new TrustManager[] { new TrustAllTrustManager() }, null);
    }
  }
}
