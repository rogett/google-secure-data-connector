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
package com.google.dataconnector.util;

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.ResourceRuleValidator;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Client Guice module.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientGuiceModule extends AbstractModule {
  
  private static final Logger LOG = Logger.getLogger(ClientGuiceModule.class);

  private String[] args; // command line args
  
  /**
   * Creates the guice module using the VMs command line arguments.
   * 
   * @param args command line.
   */
  public ClientGuiceModule(String[] args) {
    this.args = args;
  }

  /** Use default binding */
  @Override
  protected void configure() {}
  
  /** 
   * Provides the runtime for methods needing to make processes.
   * 
   * @return the VM's runtime.
   */
  @Provides
  public Runtime getRuntime() {
    return Runtime.getRuntime();
  }
  
  /**
   * Provider method that builds the {@link LocalConf} bean from the commandline arguments and 
   * configuration file.  
   * 
   * @return a populated local configuration bean.
   */
  @Provides @Singleton
  public LocalConf getLocalConf() {
    LocalConf localConf = new LocalConf();
    
    // Load configuration file and command line flags into beans
    BeanCliHelper beanCliHelper = new BeanCliHelper();
    beanCliHelper.register(localConf);
    try {
      beanCliHelper.parse(args);
      
      // validate localconf
      LocalConfValidator localConfValidator = new LocalConfValidator();
      localConfValidator.validate(localConf);
    } catch (ConfigurationBeanException e) {
      LOG.fatal("Configuration Error.", e);
      System.exit(-1);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
    } catch (LocalConfException e) {
      LOG.fatal("Configuration Error.", e);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
      System.exit(-1);
    }
    return localConf;
  }
  
  /**
   * Provider method that returns a list of {@link ResourceRule} that is read from the file 
   * system location pointed to by the {@link LocalConf} bean.  It also prepares the resource
   * rules for runtime.
   * 
   * @param localConf the local configuration.
   * @param resourceRuleUtil the resource rule util used for validation and setup of resource rules.
   * @param healthzRequestHandler the singleton HealthzRequestHandler
   * @return the created List of resources.
   */
  @Provides @Singleton
  public List<ResourceRule> getResourceRules(LocalConf localConf,
      ResourceRuleUtil resourceRuleUtil, HealthzRequestHandler healthzRequestHandler) {
  
    try {
      List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
          new FileUtil().readFile(localConf.getRulesFile()));
      
      // Add System resource rules to the list
      LOG.info("Adding healthz service rule");
      resourceRules.add(resourceRuleUtil.createHealthzRule(localConf.getUser(),
          localConf.getDomain(), localConf.getClientId(), healthzRequestHandler.getPort()));
      
      // Validate Resource Rules
      ResourceRuleValidator resourceRuleValidator = new ResourceRuleValidator();
      resourceRuleValidator.validate(resourceRules);
      
      // Remove all the rules in the config that arent for this client.
      resourceRuleUtil.getThisClientRulesAndSetId(resourceRules, localConf.getClientId());
      
      // Set the socks server port for the rules based on the local configuration.
      resourceRuleUtil.setSocksServerPort(resourceRules,
          localConf.getSocksServerPort());
      // We set all the rules to the same proxy port now that we use apache.
      resourceRuleUtil.setHttpProxyPorts(resourceRules, localConf.getHttpProxyPort());
      resourceRuleUtil.setSecretKeys(resourceRules);
      return resourceRules;
    } catch (ResourceException e) {
      LOG.fatal("Configuration Error.", e);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
      System.exit(-1); 
    } catch (IOException e) {
      LOG.fatal("Configuration File Error.", e);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
      System.exit(-1); 
    }
    return null; // HACK - VM doesnt know that System.exit() will never return.
  }
  
  /**
   * Provider method that sets up our own local SSL context and returns a SSLSocketFactory 
   * with keystore and password set by our flags.
   * 
   * @param localConfiguration the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  @Provides @Singleton
  public SSLSocketFactory getSslSocketFactory(LocalConf localConfiguration) {
    LOG.info("Using SSL for client connections.");
    
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
      if (context.getSocketFactory() == null) {
        throw new GeneralSecurityException("socketFactory not created. ");
      }
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      LOG.fatal("SSL setup error.", e);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
      System.exit(-1);
    } catch (IOException e) {
      LOG.fatal("Keystore file error.", e);
      // Guice produces SUPER VERBOSE errors that make system admins cry.  we
      // instead exit early so the message is clear.  I hate this.
      System.exit(-1);
    }
    return null;
  }
  
  /**
   * creates a singleton instance of {@link HealthzRequestHandler} with a 
   * ServerSocket listening on an ephemeral port.
   * 
   * @return created singleton instance of HealthzRequestHandler
   * @throws IOException thrown if ServerSocket couldn't be created
   */
  @Provides @Singleton
  public HealthzRequestHandler getHealthzRequestHandler() throws IOException {
    return new HealthzRequestHandler(new ServerSocket(0));
  }
}
