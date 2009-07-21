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

import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.Rfc1929SdcAuthenticator;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.sourceforge.jsocks.SOCKS;
import net.sourceforge.jsocks.socks.ProxyServer;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

  /* Dependencies */
  private LocalConf localConfiguration;
  private Rfc1929SdcAuthenticator rfc1929SdcAuthenticator;
  private Properties socksProperties;

  // Bind address
  private InetAddress bindAddress;

  // Socks Server Properties

  /**
   * Configures the SOCKS User/Password authenticator based on the rules provided
   *
   * @param localConfiguration the local configuration object.   
   * @param rfc1929SdcAuthenticator the 
   * @param socksProperties 
   */
  @Inject
  public JsocksStarter(LocalConf localConfiguration, 
      Rfc1929SdcAuthenticator rfc1929SdcAuthenticator, 
      @Named("Socks Properties") Properties socksProperties) {
    this.localConfiguration = localConfiguration;
    this.rfc1929SdcAuthenticator = rfc1929SdcAuthenticator;
    this.socksProperties = socksProperties;
  }
  
  /**
   * Do runtime configuration and start jsocks proxy thread.
   */
  public void startJsocksProxy() {
    
    // Resolve our bind host which should normally be localhost.
    try {
      bindAddress = InetAddress.getByName(LocalConf.DEFAULT_SOCKS_BIND_HOST);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Couldnt lookup bind host", e);
    }
    
    setDaemon(true);
    setName("jsocks-starter-thread");
    start();
  }
  
  @Override
  public void run() {
    // JSOCKS is configured in a static context
    SOCKS.serverInit(socksProperties);
    ProxyServer server = new ProxyServer(rfc1929SdcAuthenticator);
    LOG.info("Starting JSOCKS listener thread on port " + localConfiguration.getSocksServerPort());
    server.start(localConfiguration.getSocksServerPort(), 5, bindAddress);
  }
}
