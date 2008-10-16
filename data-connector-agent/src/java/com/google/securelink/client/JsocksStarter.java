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
package com.google.securelink.client;

import com.google.securelink.util.ClientConf;
import com.google.securelink.util.ResourceConfigEntry;
import com.google.securelink.util.ResourceConfigException;
import com.google.securelink.util.SocketResourceConfigEntry;
import com.google.securelink.util.UriResourceConfigEntry;
import com.google.securelink.util.SocketResourceConfigEntry.SocketInfo;

import net.sourceforge.jsocks.SOCKS;
import net.sourceforge.jsocks.socks.ProxyServer;
import net.sourceforge.jsocks.socks.server.UserPasswordAuthenticator;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Configures and starts the Jsocks Socks proxy.  Configuration is obtained from the
 * {@link ClientConf} object.
 *
 * @author rayc@google.com (Ray Colline)
 */
public final class JsocksStarter extends Thread {

  // Logging instance
  private static final Logger log = Logger.getLogger(JsocksStarter.class);

  private static final String LOCALHOST = "127.0.0.1";

  // Global Secure link client configuration.
  private ClientConf clientConf;

  // Socks V5 User/Password authenticator object.
  private UserPasswordAuthenticator authenticator;

  // Bind address
  private InetAddress bindAddress;

  /**
   * Configures the SOCKS User/Password authenticator based on the rules in {@link ClientConf}
   *
   * @param clientConf a populated client configuration object.
   */
  public JsocksStarter(final ClientConf clientConf) {
    this.clientConf = clientConf;
    authenticator = new UserPasswordAuthenticator();
    for (ResourceConfigEntry entry : clientConf.getRules()) {
      if (entry instanceof SocketResourceConfigEntry) {
        SocketInfo socketInfo;
        try {
          socketInfo = new SocketInfo(entry.getPattern());
        } catch (ResourceConfigException e) {
          throw new RuntimeException("Invalid Socket Pattern : entry.getPattern()");
        }
        authenticator.add(entry.getSecurityKey().toString(), socketInfo.getHostAddress(),
            socketInfo.getPort());
      } else if (entry instanceof UriResourceConfigEntry) {
        /* We setup a proxy rule for every URI resource as we use SOCKS authentication to
         * password protect each of the URL patterns.
         */
        authenticator.add(entry.getSecurityKey().toString(), LOCALHOST, entry.getPort());
      }
    }
    try {
      bindAddress = InetAddress.getByName(clientConf.getSocksdBindHost());
    } catch (UnknownHostException e) {
      throw new RuntimeException("Couldnt lookup bind host", e);
    }
  }

  @Override
  public void run() {
    // JSOCKS is configured in a static context
    SOCKS.serverInit(clientConf.getClientProps());
    ProxyServer server = new ProxyServer(authenticator);
    log.info("Starting JSOCKS listener thread on port " + clientConf.getSocksServerPort());
    server.start(clientConf.getSocksServerPort(), 5, bindAddress);
  }
}