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

import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.ResourceConfigEntry;
import com.google.dataconnector.util.UriResourceConfigEntry;
import com.google.dataconnector.wpgprocessors.SecureLinkHttpFilter;

import com.wpg.proxy.Proxy;
import com.wpg.proxy.ProxyRegistry;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public final class WpgProxyStarter {

  // Logging instance
  private static final Logger log = Logger.getLogger(WpgProxyStarter.class);
  
  // Secure Link client configuration
  private ClientConf clientConf;
  
  /**
   * Creates the wpg proxy starter class with the supplied configuration.
   * 
   * @param clientConf Secure Link configuration object.
   */
  public WpgProxyStarter(final ClientConf clientConf) {
    this.clientConf = clientConf;
  }

  /**
   * Creates a proxy thread per HTTP rule listed in the client configuration object.
   */
  public void startHttpProxy() {
    for (ResourceConfigEntry entry : clientConf.getRules()) {
      // Skip all non http rule entries these will be handled elsewhere.
      if (!(entry instanceof UriResourceConfigEntry)) {
        continue;
      }
      /* Each proxy instance can filter multiple URL patterns, but in our current setup we
       * associate only one URL per thread and let the underlying SOCKS authentication provide
       * the password protection.
       * 
       * TODO(rayc) update to need only one proxy thread with password protection done with HTTP
       * proxy auth.
       */
      List<String> proxyFilterRules = new ArrayList<String>();
      proxyFilterRules.add(entry.getPattern());
      ProxyRegistry proxyRegistry = new ProxyRegistry();
      proxyRegistry.addRequestProcessor(new SecureLinkHttpFilter(proxyFilterRules));
      Proxy httpProxyService;
      try {
        httpProxyService = new Proxy(InetAddress.getByName(clientConf.getHttpProxyBindHost()), 
            entry.getPort(), 0,  proxyRegistry);
      } catch (UnknownHostException e) {
        String msg = "Unknown host exception for " + clientConf.getHttpProxyBindHost() + 
            "  Serious configuration problem!";
        log.fatal(msg);
        throw new RuntimeException(msg, e);
      }
      log.info("Starting http proxy server on port " + entry.getPort());
      httpProxyService.setName("Proxy Thread Port " + entry.getPort() + "(" + 
          entry.getPattern() + ")");
      httpProxyService.start();  // TODO(rayc) maintain list of threads in a global object.
    }
  }
}
