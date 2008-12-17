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
import com.google.dataconnector.util.ApacheSetupException;
import com.google.dataconnector.util.ClientGuiceModule;
import com.google.dataconnector.util.AgentConfigurationException;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

/**
 * Entry point class for starting the Secure Data Connector.  There are three components to the
 * Secure Data Connector: 
 * 
 * 1) The "secure data connection" to the server which provides the transport from the server
 * side back to the client.  see {@link SecureDataConnection}
 * 2) The Socks 5 proxy which provides the network firewall to incoming network connections through
 * the secure data transport. see {@link JsocksStarter}
 * 3) The HTTP(S) proxy which provides the http firewall filtering for incoming http requests.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Client {
  
  // Logging instance
  private static final Logger log = Logger.getLogger(Client.class);

  /* Dependencies */
  private LocalConf localConfiguration;
  private JsocksStarter jsocksStarter;
  private ApacheStarter apacheStarter;
  private SecureDataConnection secureDataConnection;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param localConfiguration the local configuration object.
   * @param resourceRules runtime configured resource rules.
   * @param sslSocketFactory 
   * @param apacheStarter 
   * @param jsocksStarter 
   * @param secureDataConnection 
   */
  @Inject
  public Client(LocalConf localConfiguration, List<ResourceRule> resourceRules, 
      SSLSocketFactory sslSocketFactory, ApacheStarter apacheStarter, 
      JsocksStarter jsocksStarter, SecureDataConnection secureDataConnection) {
    this.localConfiguration = localConfiguration;
    this.apacheStarter = apacheStarter;
    this.jsocksStarter = jsocksStarter;
    this.secureDataConnection = secureDataConnection;
  }
  
  /**
   * Starts three components in separate threads.
   * 
   * @throws IOException if any socket communication issues occur.
   * @throws ConnectionException if login is incorrect or other Woodstock connection errors.
   * @throws ApacheSetupException if apache has any errors.
   */
  public void startUp() throws IOException, ConnectionException, ApacheSetupException {
    
    // Set client logging properties.
    Properties properties = new Properties();
    properties.load(new ByteArrayInputStream(
        localConfiguration.getLogProperties().trim().getBytes()));
    PropertyConfigurator.configure(properties);
    
    apacheStarter.startApacheHttpd();
    jsocksStarter.startJsocksProxy();
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
    final Injector injector = Guice.createInjector(new ClientGuiceModule(args));
    // Create the client instance and start services
    Client client = injector.getInstance(Client.class);
    try {
      client.startUp();
    } catch (IOException e) {
      log.fatal("Connection error.", e);
    } catch (ConnectionException e) {
      log.fatal("Client connection failure.", e);
    } catch (AgentConfigurationException e) {
      log.fatal("Client configuration error.", e);
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
    props.setProperty("log4j.rootLogger","info, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "%d [%t] %-5p %c %x - %m%n");
    return props;
  }
}
