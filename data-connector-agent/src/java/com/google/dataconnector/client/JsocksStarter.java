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

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.SocketInfo;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.Rfc1929SdcAuthenticator;
import com.google.inject.Inject;

import net.sourceforge.jsocks.SOCKS;
import net.sourceforge.jsocks.socks.ProxyServer;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

/**
 * Configures and starts the Jsocks Socks proxy.  Configuration is obtained from the {@LocalConf}
 * object.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class JsocksStarter extends Thread {

  // Logging instance
  private static final Logger LOG = Logger.getLogger(JsocksStarter.class);

  private static final String LOCALHOST = "127.0.0.1";

  /* Dependencies */
  private LocalConf localConfiguration;
  private List<ResourceRule> resourceRules;
  private ResourceRuleUtil resourceRuleUtil;

  // Socks V5 User/Password authenticator object.
  private Rfc1929SdcAuthenticator authenticator;

  // Bind address
  private InetAddress bindAddress;

  // Socks Server Properties
  private Properties socksProperties;


  /**
   * Configures the SOCKS User/Password authenticator based on the rules provided
   *
   * @param localConfiguration the local configuration object.   
   * @param resourceRules the rule sets.
   */
  @Inject
  public JsocksStarter(LocalConf localConfiguration, List<ResourceRule> resourceRules,
      ResourceRuleUtil resourceRuleUtil) {
    this.localConfiguration = localConfiguration;
    this.resourceRules = resourceRules;
    this.resourceRuleUtil = resourceRuleUtil;
  }
  
  /**
   * Do runtime configuration and start jsocks proxy thread.
   */
  public void startJsocksProxy() {
    // Create firewall rules in jsocks proxy.
    authenticator = new Rfc1929SdcAuthenticator();
    for (ResourceRule resourceRule : resourceRules) {
      if (resourceRule.getPattern().startsWith(ResourceRule.SOCKETID)) {
        SocketInfo socketInfo;
        try {
          socketInfo = new SocketInfo(resourceRule.getPattern());
        } catch (ResourceException e) {
          throw new RuntimeException("Invalid Socket Pattern : entry.getPattern()");
        }
        authenticator.add(resourceRule.getSecretKey().toString(), socketInfo.getHostAddress(),
            socketInfo.getPort());
      } else if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID) || 
          (resourceRule.getPattern().startsWith(ResourceRule.HTTPSID))) {
        authenticator.add(resourceRule.getSecretKey().toString(), 
            resourceRuleUtil.getHostnameFromRule(resourceRule), 
            resourceRuleUtil.getPortFromRule(resourceRule));
        LOG.debug("Added rule " + resourceRule.getPattern() + " host: " + 
            resourceRuleUtil.getHostnameFromRule(resourceRule) + " port: " + 
            resourceRuleUtil.getPortFromRule(resourceRule));
      }
      LOG.info("Adding rule: " + resourceRule.getPattern());
    }
    
    // Resolve our bind host which should normally be localhost.
    try {
      bindAddress = InetAddress.getByName(LocalConf.DEFAULT_SOCKS_BIND_HOST);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Couldnt lookup bind host", e);
    }
    
    // Load properties from LocalConf.
    socksProperties = new Properties();
    try {
    socksProperties.load(
        new ByteArrayInputStream(localConfiguration.getSocksProperties().trim().getBytes()));
    } catch (IOException e) {
      throw new RuntimeException("Invalid socks properties", e);
    }
    setName("jsocks-starter-thread");
    setDaemon(true);
    start();
  }
  
  @Override
  public void run() {
    // JSOCKS is configured in a static context
    SOCKS.serverInit(socksProperties);
    ProxyServer server = new ProxyServer(authenticator);
    LOG.info("Starting JSOCKS listener thread on port " + localConfiguration.getSocksServerPort());
    server.start(localConfiguration.getSocksServerPort(), 5, bindAddress);
  }
}
