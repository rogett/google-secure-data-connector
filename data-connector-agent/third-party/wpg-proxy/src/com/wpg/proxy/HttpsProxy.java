/*
 * Java HTTP Proxy Library (wpg-proxy), more info at
 * http://wpg-proxy.sourceforge.net/
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * 
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package com.wpg.proxy;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class is used for listening to a preconfigured port for proxy connection
 * requests and invokes processors to process each client request. It doesn't
 * have anything pertinent to Https per se so it is somewhat a misnomer. It
 * differs from Proxy class, however, in the way networking is done. This class
 * uses older blocking IO sockets whereas Proxy class is based on NIO. This was
 * a temporary solution to get Https fixed. TODO(enaoumov) Get it right. This
 * class may become redundant if we refactor Https processing to use NIO. At the
 * very least it should be renamed to reflect its true nature.
 * 
 */

class HttpsProxy extends Thread {

  /** Logger object used for all logging activities. */
  private static Logger logger = Logger.getLogger(HttpsProxy.class);

  /** Port to run proxy on. */
  private int port;

  /** backlog value. */
  private int backlog;

  /** Server running status flag. */
  private boolean running = false;

  /** Configuration object for the proxy. */
  private ProxyRegistry proxyRegistry;

  /** Configuration for the KeyStore. */
  private KeyStoreConfig keyStoreConfig;

  /** Bind host, used for multihomed systems. Defaults to localhost */
  private String host = "localhost";

  /**
   * Is the server running?
   * 
   * @return running flag
   */
  public boolean isRunning() {
    return running;
  }

  /** Stop the proxy server. */
  public void shutdown() {
    running = false;
  }

  /**
   * Creates a new Https proxy.
   * 
   * @param port - port to listen on
   * @param backlog - number of awaiting requests to queue
   * @param registry - configuration object for the proxy
   * @param ksc - configuration for the KeyStore
   */
  public HttpsProxy(final int port, final int backlog,
      final ProxyRegistry registry, final KeyStoreConfig ksc) {
    super("HttpsProxy");
    setPort(port);
    setBacklog(backlog);
    // stats.setProxy(this);
    this.proxyRegistry = registry;
    this.keyStoreConfig = ksc;
  }

  /**
   * Set the port to listen for new requests on.
   * 
   * @param p - port value
   */
  public void setPort(final int p) {
    port = p;
  }

  /**
   * Set the backlog, or number of awaiting requests to queue.
   * 
   * @param bl - backlog value
   */
  public void setBacklog(final int bl) {
    backlog = bl;
  }

  /**
   * Get the port to listen for new requests on.
   * 
   * @return port value
   */
  public int getPort() {
    return port;
  }

  /**
   * Get the backlog, or number of awaiting requests to queue.
   * 
   * @return backlog value
   */
  public int getBacklog() {
    return backlog;
  }

  /**
   * Get the binding host.
   * 
   * @return host value
   */
  public String getHost() {
    return host;
  }

  /**
   * Set the bind host.
   * 
   * @param host - host to start the proxy at.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /** Thread's run method. */
  @Override
  public void run() {
    logger.info("Proxy started on: " + host + ":" + port);
    running = true;
    ServerSocket serverSocket = null;
    try {
      serverSocket =
          new ServerSocket(port, backlog, InetAddress.getByName(host));
    } catch (IOException e) {
      logger.error("Could not listen on port: " + port);
      return;
    }
    while (running) {
      Socket clientSocket = null;
      try {
        clientSocket = serverSocket.accept();
        new HttpsProxyProcessor(clientSocket, proxyRegistry, port,
            keyStoreConfig);
      } catch (IOException e) {
        logger.error("Error processing client: " + clientSocket
            + " Exception: " + e, e);
        try {
          clientSocket.close();
          shutdown();
        } catch (IOException ioe) {
          logger.error("Error closing socket: " + clientSocket + " Exception: "
              + e, e);
        }
      }
    }
  }
}
