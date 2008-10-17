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

import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.ConfigurationException;
import com.google.dataconnector.util.ConnectionException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;

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
  private ClientConf clientConf;

  /** Jsocks Starter Object */
  private JsocksStarter jsocksStarter;
  
  /** WPG Proxy Starter Object */
  private WpgProxyStarter wpgProxyStarter;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param clientConf the client configuration object.
   */
  public Client(final ClientConf clientConf) {
    this.clientConf = clientConf;
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
    if (clientConf.getUseSsl()) {
      sslSocketFactory = getSslSocketFactory(clientConf);
    }
      
    wpgProxyStarter = new WpgProxyStarter(clientConf);
    wpgProxyStarter.startHttpProxy();
    jsocksStarter = new JsocksStarter(clientConf);
    jsocksStarter.start(); 
    SecureDataConnection secureDataConnection = new SecureDataConnection(clientConf, 
        sslSocketFactory);
    secureDataConnection.connect();
  }
  
  /**
   * Entry point for the Secure Data Connector binary.  Sets up logging, parses flags and
   * creates ClientConf.
   * 
   * @param args
   */
  public static void main(String[] args) {
    // Bootstrap logging system
    PropertyConfigurator.configure(getBootstrapLoggingProperties());
    
    // Parse Flags
    ClientConf.parseFlags(args);

    // Read in client properties file
    Properties clientProps = new Properties();
    String clientPropsFileName = ClientConf.getConfigFileFlagValue();
    try {
      clientProps = ClientConf.loadPropertiesFile(clientPropsFileName);
    } catch (IOException e) {
      log.fatal("Could not load properties file " + clientPropsFileName + " exception: " + e);
      System.exit(1);
    } 

    try {
      // Populate client configuration object
      ClientConf clientConf = new ClientConf(clientProps);

      // Load logging properties file.
      PropertyConfigurator.configure(clientConf.getLoggingProperties());

      // Create the client instance and start services
      Client client = new Client(clientConf);
      client.startUp();
    } catch (ConnectionException e) {
      log.fatal("Connection error: " + e.getMessage());
      System.exit(1);
    } catch (ConfigurationException e) {
      log.fatal(e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      log.fatal("Client connection failure: ", e);
    }
  }
  
  /**
   * Returns a base set of logging properties so we can log fatal errors before config parsing is 
   * done.
   * 
   * @return Properties a basic console logging setup.
   */
  public static Properties getBootstrapLoggingProperties() {
    Properties props = new Properties();
    props.setProperty("log4j.rootLogger","debug, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "%-4r [%t] %-5p %c %x - %m%n");
    return props;
  }
  
  /**
   * Sets up our own local SSL context and returns a SSLSocketFactory with keystore and password
   * set by our flags.
   * 
   * @param clientConf the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  public static SSLSocketFactory getSslSocketFactory(ClientConf clientConf) {
    char[] password = clientConf.getSslKeyStorePassword().toCharArray();
    try {
      // Get a new "Java Key Store"
      KeyStore keyStore = KeyStore.getInstance("JKS");
      // Load with our trusted certs and setup the trust manager(the server).
      keyStore.load(new FileInputStream(clientConf.getSslKeyStoreFile()), password);
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
      tmf.init(keyStore);
      // Create the SSL context with our private store.
      SSLContext context = SSLContext.getInstance("TLSv1");
      context.init(null, tmf.getTrustManagers(), null); 
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("SSL setup error: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Could read Keystore file: " + e);
    }
  }
  
}
