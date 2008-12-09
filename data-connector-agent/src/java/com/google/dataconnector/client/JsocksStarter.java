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
import com.google.dataconnector.registration.v2.SocketInfo;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Inject;

import net.sourceforge.jsocks.SOCKS;
import net.sourceforge.jsocks.socks.ProxyServer;
import net.sourceforge.jsocks.socks.server.UserPasswordAuthenticator;
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
public final class JsocksStarter extends Thread {

  // Logging instance
  private static final Logger LOG = Logger.getLogger(JsocksStarter.class);

  private static final String LOCALHOST = "127.0.0.1";

  /* Dependencies */
  private LocalConf localConfiguration;
  private List<ResourceRule> resourceRules;
  private ResourceRuleUtil resourceRuleUtil;

  // Socks V5 User/Password authenticator object.
  private UserPasswordAuthenticator authenticator;

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
    authenticator = new UserPasswordAuthenticator();
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
      } else if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        /* We setup a proxy rule for every URI resource as we use SOCKS authentication to
         * password protect each of the URL patterns.
         */
        authenticator.add(resourceRule.getSecretKey().toString(), LOCALHOST, 
            resourceRule.getHttpProxyPort());
      } else if (resourceRule.getPattern().startsWith(ResourceRule.HTTPSID)) {
        authenticator.add(resourceRule.getSecretKey().toString(), 
            resourceRuleUtil.getHostnameFromRule(resourceRule), 
            resourceRuleUtil.getPortFromRule(resourceRule));
        LOG.info("Added https rule " + resourceRule.getPattern() + " host: " + 
            resourceRuleUtil.getHostnameFromRule(resourceRule) + " port: " + 
            resourceRuleUtil.getPortFromRule(resourceRule));
      }
      LOG.info("Adding rule: " + resourceRule.getPattern());
    }
    
    // Resolve our bind host which should normally be localhost.
    try {
      bindAddress = InetAddress.getByName(localConfiguration.getSocksdBindHost());
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