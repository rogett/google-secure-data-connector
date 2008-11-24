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

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.ResourceRuleValidator;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.LocalConfException;
import com.google.dataconnector.util.LocalConfValidator;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Sets up client to run stand-alone by parsing flags and reading in configuration.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientMain {
  
  // Logging instance
  private static final Logger log = Logger.getLogger(ClientMain.class);
  
  private String[] args;
  
  /**
   * Initializes the entry point class with the command line arguments.
   * 
   * @param args command line arguments.
   */
  public ClientMain(String[] args) {
    this.args = args;
  }
  
  /**
   * Initializes the runtime environment for the Secure Data Connector and starts the 
   * {@link Client}.
   */
  public void init() {
    // Bootstrap logging system
    PropertyConfigurator.configure(getBootstrapLoggingProperties());
    
    // Create configuration beans
    LocalConf localConfiguration = new LocalConf();
    
    try {
      // Load configuration file and command line flags into beans
      BeanCliHelper beanCliHelper = new BeanCliHelper();
      beanCliHelper.register(localConfiguration);
      beanCliHelper.parse(args);
    
      // validate localconf
      LocalConfValidator localConfValidator = new LocalConfValidator();
      localConfValidator.validate(localConfiguration);
      
      // Create resource rules based on rules config
      ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil();
      List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
          loadFileIntoString(localConfiguration.getRulesFile()));
      
      // Validate Resource Rules
      ResourceRuleValidator resourceRuleValidator = new ResourceRuleValidator();
      resourceRuleValidator.validate(resourceRules);
      
      // Set runtime configuration.
      resourceRuleUtil.setSocksServerPort(resourceRules,
          localConfiguration.getSocksServerPort());
      resourceRuleUtil.setHttpProxyPorts(resourceRules, 
          localConfiguration.getStartingHttpProxyPort());
      resourceRuleUtil.setSecretKeys(resourceRules);
      
      // Load logging properties file.
      Properties properties = new Properties();
      properties.load(new ByteArrayInputStream(
          localConfiguration.getLogProperties().trim().getBytes()));
      PropertyConfigurator.configure(properties);

      // Create the client instance and start services
      Client client = new Client(localConfiguration, resourceRules);
      client.startUp();
    } catch (ConnectionException e) {
      log.fatal("Connection error.", e);
    } catch (IOException e) {
      log.fatal("Client connection failure.", e);
    } catch (LocalConfException e) {
      log.fatal("Local configuration error.", e);
    } catch (ConfigurationBeanException e) {
      log.fatal("Local configuration error.", e);
    } catch (ResourceException e) {
      log.fatal("Resource rules error.", e);
    } 
  }
  
  /**
   * Entry point for the Secure Data Connector binary.  Sets up logging, parses flags and
   * creates ClientConf.
   * 
   * @param args
   */
  public static void main(String[] args) {
    ClientMain clientMain = new ClientMain(args);
    clientMain.init();
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
   * Helper that loads a file into a new string.
   * 
   * @param fileName properties file with rules configuration.
   * @returns a String representing the file contents.
   * @throws IOException if any errors are encountered reading the file
   */
  public static String loadFileIntoString(final String fileName) throws IOException {
    File file = new File(fileName);
    byte[] buffer = new byte[(int) file.length()];
    InputStream fin = new FileInputStream(fileName);
    fin.read(buffer);
    return new String(buffer);
  }
}
