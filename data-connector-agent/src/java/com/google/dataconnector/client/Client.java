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
 *
 * $Id$
 */
package com.google.dataconnector.client;

import com.google.dataconnector.util.ClientGuiceModule;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.LocalConfException;
import com.google.dataconnector.util.LocalConfValidator;
import com.google.dataconnector.util.ShutdownManager;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

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
  
  // Constants
  private static final long MAX_BACKOFF_TIME = 5 * 60 * 1000; // 5 minutes

  /* Dependencies */
  private final LocalConf localConf;
  private final SdcConnection secureDataConnection;
  private final JsocksStarter jsocksStarter;
  private final ShutdownManager shutdownManager;
  
  /* Local fields */
  private static long unsuccessfulAttempts = 0;


  /**
   * Creates a new client from the populated client configuration object.
   */
  @Inject
  public Client(final LocalConf localConf, final SdcConnection secureDataConnection,
      final JsocksStarter jsocksStarter,
      final ShutdownManager shutdownManager) {
    this.localConf = localConf;
    this.secureDataConnection = secureDataConnection;
    this.jsocksStarter = jsocksStarter;
    this.shutdownManager = shutdownManager; 
  }

  /**
   * Reads flags and config files, validates configuration and starts agent.
   */
  public void parseFlagsValidateAndConnect(final String[] args) {
    
    // validate the localConf.xml file and the input args
    try {
      BeanCliHelper beanCliHelper = new BeanCliHelper();
      beanCliHelper.register(localConf);
      beanCliHelper.parse(args);
      new LocalConfValidator().validate(localConf);
    } catch (LocalConfException e) {
      LOG.fatal("Configuration error", e);
      return;
    } catch (ConfigurationBeanException e) {
      LOG.fatal("Configuration error", e);
      return;
    }
    
    // Set log4j properties.  We do not expect this file to change often so we do not use the
    // cooler, yet more resource intensive, "configureAndWatch".
    PropertyConfigurator.configure(localConf.getLog4jPropertiesFile());
    if (localConf.getDebug()) {
      Logger.getRootLogger().setLevel(Level.DEBUG);
    }
    
    
    // Connect
    try {
      // If the password file is specified, then read its contents and override
      // the password property with the contents read.  At this point the file
      // is readable since the check has already been performed during validation.
      if (localConf.getPasswordFile() != null) {
      	String password = new FileUtil().readFile(localConf.getPasswordFile());
      	localConf.setPassword(password);
      }
      // start jsocks thread
      jsocksStarter.startJsocksProxy();
      // start main processing thread - to initiate connection/registration with the SDC server
      secureDataConnection.connect();
    } catch (IOException e ) {
      LOG.fatal("Cannot read password file.", e);
    }	catch (ConnectionException e) {
      LOG.fatal("Connection failed.", e);
    } finally {
      shutdownManager.shutdownAll();
    }
    
    // Check whether connection was successful or not.
    if (secureDataConnection.hasConnectedSuccessfully()) {
      unsuccessfulAttempts = 0;
    } else if (localConf.getStartOnce()) {
      LOG.info("Configured only to start once. Quitting!");
      unsuccessfulAttempts = -1; // Sentinel value meaning we should quit.
    } else { // Failed connection
      unsuccessfulAttempts++;
    }
  }
  
  /**
   * Entry point for the Secure Data Connector binary.  Sets up logging, parses flags and
   * creates ClientConf.
   *
   * @param args
   */
  public static void main(final String[] args) {
    // Bootstrap logging system
    PropertyConfigurator.configure(getBootstrapLoggingProperties());

    final Injector injector = ClientGuiceModule.getInjector();
    final ShutdownManager shutdownManager = injector.getInstance(ShutdownManager.class);
    // Add shutdown hook to call shutdown if control c or OS SIGTERM is received.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        shutdownManager.shutdownAll();
      }
    });
    
    // Starts the client in a loop that exponentially backs off. 
    while (true) {
      try {
        // Only try to back-off if we have unsuccessful connections.
        if (unsuccessfulAttempts > 0) {
          long backOffTime = Math.min(MAX_BACKOFF_TIME, (1 << unsuccessfulAttempts) * 1000L);
          try {
            // Sleep for the amount of time needed.
            Thread.sleep(backOffTime);
          } catch (InterruptedException e) {
            LOG.error("Interrupted while trying to exponentially back off. Exiting.");
            break;
          }
          LOG.info("Starting agent after " + unsuccessfulAttempts + " unsuccessful attempts." +
              " Next connect in " + backOffTime + " milliseconds.");
        } else if (unsuccessfulAttempts == -1) {
          // We are being told to quit probably because we were only configured to start once.
          break; 
        }
        injector.getInstance(Client.class).parseFlagsValidateAndConnect(args);
      } catch (Exception e) { // This is an outside server loop. Catch everything.
        LOG.error("Agent died.", e);
        shutdownManager.shutdownAll();
      }
    }
  }
  
  /**
   * Returns a base set of logging properties so we can log fatal errors before config parsing is
   * done.
   *
   * @return Properties a basic console logging setup.
   */
  public static Properties getBootstrapLoggingProperties() {
    final Properties props = new Properties();
    props.setProperty("log4j.rootLogger","info, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.Ant d.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "%d [%t] %-5p %c %x - %m%n");
    return props;
  }
}
