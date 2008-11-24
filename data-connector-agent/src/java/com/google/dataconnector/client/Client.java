/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.client;

import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Entry point class for starting the Secure Data Connector.  There are three components to the
 * Secure Data Connector: 
 * 
 * 1) The "secure data connection" to the server which provides the transport from the server
 * side back to the client.  see {@link SecureDataConnection}
 * 2) The Socks 5 proxy which provides the network firewall to incoming network connections through
 * the secure data transport. see {@link JsocksStarter}
 * 3) The HTTP(S) proxy which provides the http firewall filtering for incoming http requests.
 * see {@link WpgProxyStarter}
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Client {
  
  // Logging instance
  private static final Logger log = Logger.getLogger(Client.class);

  /** Secure Data Connector Configuration */
  private LocalConf localConfiguration;
  private List<ResourceRule> resourceRules;

  /** Jsocks Starter Object */
  private JsocksStarter jsocksStarter;
  
  /** WPG Proxy Starter Object */
  private WpgProxyStarter wpgProxyStarter;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param localConfiguration the local configuration object.
   * @param resourceRules runtime configured resource rules.
   */
  public Client(LocalConf localConfiguration, List<ResourceRule> resourceRules) {
    this.localConfiguration = localConfiguration;
    this.resourceRules = resourceRules;
  }
  
  /**
   * Starts three components in separate threads.
   * 
   * @throws IOException if any socket communication issues occur.
   * @throws ConnectionException if login is incorrect or other Woodstock connection errors.
   */
  public void startUp() throws IOException, ConnectionException {
    
    // Check SSL flag and leave sslSocketFactory set to null if SSL is disabled.
    SSLSocketFactory sslSocketFactory = null;
    if (localConfiguration.getUseSsl()) {
      log.info("Using SSL for client connections.");
      sslSocketFactory = getSslSocketFactory(localConfiguration);
    }
      
    wpgProxyStarter = new WpgProxyStarter(localConfiguration, resourceRules);
    wpgProxyStarter.startHttpProxy();
    jsocksStarter = new JsocksStarter(localConfiguration, resourceRules);
    jsocksStarter.startJsocksProxy();
    SecureDataConnection secureDataConnection = new SecureDataConnection(localConfiguration, 
        resourceRules, sslSocketFactory);
    secureDataConnection.connect();
  }

  /**
   * Sets up our own local SSL context and returns a SSLSocketFactory with keystore and password
   * set by our flags.
   * 
   * @param localConfiguration the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  public static SSLSocketFactory getSslSocketFactory(LocalConf localConfiguration) {
    char[] password = localConfiguration.getSslKeyStorePassword().toCharArray();
    try {
      String keystorePath = localConfiguration.getSslKeyStoreFile();
      
      SSLContext context = SSLContext.getInstance("TLSv1");
      if (keystorePath != null) { // The customer specified their own keystore.
        // Get a new "Java Key Store"
        KeyStore keyStore = KeyStore.getInstance("JKS");
        // Load with our trusted certs and setup the trust manager.
        keyStore.load(new FileInputStream(keystorePath), password);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(keyStore);
        // Create the SSL context with our private store.
        context.init(null, tmf.getTrustManagers(), null);
      } else {
        // Use the JVM default as trusted store. This would be located somewhere around
        // jdk.../jre/lib/security/cacerts, and will contain widely used CAs.
        context.init(null, null, null);  // Use JVM default.
      }
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("SSL setup error: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Could read Keystore file: " + e);
    }
  }
}
