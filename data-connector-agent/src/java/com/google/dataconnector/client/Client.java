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
package com.google.dataconnector.client;

import com.google.dataconnector.registration.v3.ResourceRule;
import com.google.dataconnector.util.ClientGuiceModule;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.HealthCheckRequestHandler;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

/**
 * Entry point class for starting the Secure Data Connector.  There are three components to the
 * Secure Data Connector: 
 * 
 * 1) The "secure data connection" to the server which provides the transport from the server
 * side back to the client.  see {@link SdcConnection}
 * 2) The Socks 5 proxy which provides the network firewall to incoming network connections through
 * the secure data transport. see {@link JsocksStarter}
 * 3) The HTTP(S) proxy which provides the http firewall filtering for incoming http requests.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Client {
  
  // Logging instance
  private static final Logger LOG = Logger.getLogger(Client.class);

  /* Dependencies */
  private LocalConf localConfiguration;
  private SdcConnection secureDataConnection;
  private JsocksStarter jsocksStarter;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param localConfiguration the local configuration object.
   * @param resourceRules runtime configured resource rules.
   * @param sslSocketFactory 
   * @param secureDataConnection
   */
  @Inject
  public Client(LocalConf localConfiguration, List<ResourceRule> resourceRules, 
      SSLSocketFactory sslSocketFactory, SdcConnection secureDataConnection,
      JsocksStarter jsocksStarter) {
    this.localConfiguration = localConfiguration;
    this.secureDataConnection = secureDataConnection;
    this.jsocksStarter = jsocksStarter;
  }
  
  /**
   * Starts 2 components in separate threads.
   * 
   * @throws IOException if any socket communication issues occur.
   * @throws ConnectionException if login is incorrect or other Woodstock connection errors.
   */
  public void startUp() throws IOException, ConnectionException {
    
    // Set log4j properties and watch for changes every min (default)
    PropertyConfigurator.configureAndWatch(localConfiguration.getLog4jPropertiesFile());
    if (localConfiguration.getDebug()) {
	  Logger.getRootLogger().setLevel(Level.DEBUG);
    }
    
    try {
      jsocksStarter.startJsocksProxy();
      secureDataConnection.connect();
    } catch (ConnectionException e) {
      LOG.debug(e);
      LOG.info("Connection exception: " + e.getMessage());
    } finally {
      LOG.info("Exiting agent.");
      System.exit(1);
    }
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
    ClientGuiceModule.setArgs(args);
    Injector injector = new ClientGuiceModule.InjectorProvider().get();
    
    try {
      // start the healthcheck service before we do anything else.
      injector.getInstance(HealthCheckRequestHandler.class).init();
      // Create the client instance and start services
      injector.getInstance(Client.class).startUp();
    } catch (IOException e) {
      LOG.fatal("Connection error.", e);
    } catch (ConnectionException e) {
      LOG.fatal("Client connection failure.", e);
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
