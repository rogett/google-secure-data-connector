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

import com.google.common.base.Preconditions;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.Rfc1929SdcAuthenticator;
import com.google.dataconnector.util.ShutdownManager;
import com.google.dataconnector.util.Stoppable;
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
public class JsocksStarter extends Thread implements Stoppable {

  // Logging instance
  private static final Logger LOG = Logger.getLogger(JsocksStarter.class);

  /* Dependencies */
  private final LocalConf localConfiguration;
  private final Rfc1929SdcAuthenticator rfc1929SdcAuthenticator;
  private final Properties socksProperties;
  private ShutdownManager shutdownManager;

  // Bind address
  private InetAddress bindAddress;
  
  // Proxy server instance
  private ProxyServer proxyServer; 

  /**
   * Configures the SOCKS User/Password authenticator based on the rules provided
   *
   * @param localConfiguration the local configuration object.
   * @param rfc1929SdcAuthenticator the
   * @param socksProperties
   */
  @Inject
  public JsocksStarter(final LocalConf localConfiguration,
      final Rfc1929SdcAuthenticator rfc1929SdcAuthenticator,
      final @Named("Socks Properties") Properties socksProperties,
      final ShutdownManager shutdownManager) {
    this.localConfiguration = localConfiguration;
    this.rfc1929SdcAuthenticator = rfc1929SdcAuthenticator;
    this.socksProperties = socksProperties;
    this.shutdownManager = shutdownManager;
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
    
    // Set thread info.
    setDaemon(true);
    setName(this.getClass().getName());
    
    start();
  }

  @Override
  public void run() {
    
    // Add to shutdown manager
    shutdownManager.addStoppable(this); 
    
    // JSOCKS is configured in a static context
    SOCKS.serverInit(socksProperties);
    proxyServer = new ProxyServer(rfc1929SdcAuthenticator);
    LOG.info("Starting JSOCKS listener thread on port " + localConfiguration.getSocksServerPort());
    proxyServer.start(localConfiguration.getSocksServerPort(), 5, bindAddress);
  }

  /**
   * Stops proxy server and issues interrupt as recommended by {@link ProxyServer#stop()}
   */
  @Override
  public void shutdown() {
    if (proxyServer == null) {
      LOG.warn("Jsocks was never started, however jsocks starter thread was.  Shutting down");
      this.interrupt(); // interrupt thread wherever it may be which should cause a stop.
      return;
    }
    // Actually stop server.
    proxyServer.stop();
    this.interrupt();  // Jsocks recommends interrupting this thread after calling stop.
  }
}
