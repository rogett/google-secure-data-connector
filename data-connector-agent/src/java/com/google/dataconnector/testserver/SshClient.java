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

package com.google.dataconnector.testserver;

import com.jcraft.jsch.*;
import org.apache.commons.logging.Log;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Implements an SSH client that forwards a port.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SshClient {
  
  // Woodstock user  TODO(rayc) make a default flag argument
  private final static String WS_USER = "woodstock";
    
  // Remote side SOCKS server.
  private final static int CUSTOMER_SOCKS_SERVER_PORT = 1080; 

  // Local side listen port for SOCKS forwards.
  private final static String DEFAULT_LOCAL_SOCKS_SERVER_PORT = "10000"; 

  // Logger
  Log log = ServerConf.log;
 
  // SSH KEY TODO(rayc) use this instead of "strictHostKeyChecking=no"
  private static String serverHostKey = 
      "AAAAB3NzaC1yc2EAAAABIwAAAQEAx+l7oHcTTt/5ATzb3ae1sHBE1zODmb6bc4Zn" +
      "X1dcbTg4WSZ7ADwzdHfKYoIXwOSWo8Pm8uEMaYPPD2XHYJNwtngHnyzYSMtuc0L+" +
      "2EumdCwjqhUA7OCJPAYJdX3VxIkySit8G41iwVnLPo2K1fYlD567rLgI9pvj+LIL" +
      "dD+RMASQCAmfO2qlBDtiD3NB6dA+MNlObqxR34nBZTJi8GNSztVkI+zcbKMd45E1" +
      "26xjpQ6uGSaZ3AdhdKy7pJlgHiHJ5IuBxFmvHZAGN0DVpPZrUy0SDjKWsoBEyFhx" +
      "8x/wi2+DzsuNJCQ5MS3zpZKN3xtkTFoJmVbVaDaTCw2Czs7uwQ==";

  /**
   * Creates ssh layer connection using JSCH.
   * 
   * @param socket an already connected socket.
   * 
   * @throws JSchException if Identity add fails
   *                       if SSH negotiation or other protocol error occurs.
   */
  public SshClient(Socket socket) throws JSchException {
    
    JSch jsch = new JSch();
    JSch.setLogger(new TestSshClientLogger());
    jsch.addIdentity(ServerConf.flags.getOptionValue("key"));
    Session jschSession = jsch.getSession(WS_USER, "", 0);
    
    /* Set crypto algorithm to use blowfish which appears fastest
     * TODO(rayc) use null encryption for data transmission
     */
    jschSession.setConfig("cipher.s2c", "blowfish-cbc");
    jschSession.setConfig("cipher.c2s", "blowfish-cbc");
    
    // TODO(rayc) implement key checking based on "serverHostKey" var above.
    jschSession.setConfig("StrictHostKeyChecking", "no");

    /* 
     * Connects to server and creates socketFactory that JSch 
     * can use to access this socket.
     */
    SshClientSocketFactory scsf = new SshClientSocketFactory(socket);
    jschSession.setSocketFactory(scsf);
    UserInfo ui = jschSession.getUserInfo();
    jschSession.connect();
    log.info("Connected to remote side");
    
    /*
     * Setup port forwarding using SSH from a local port to the 
     * tunnel-service client's SOCKS server.  This is port advertised to 
     * services wishing to traverse the firewall.
     */
    jschSession.setPortForwardingL(
        CUSTOMER_SOCKS_SERVER_PORT, 
        "127.0.0.1", 
        new Integer(
            ServerConf.flags.getOptionValue(
                "localSocksPort", DEFAULT_LOCAL_SOCKS_SERVER_PORT)));
    log.info("Setup local port forward from " + 
        ServerConf.flags.getOptionValue("localSocksPort") + 
        " to " + CUSTOMER_SOCKS_SERVER_PORT);
    try {
      Thread.sleep(10000000);
    } catch (InterruptedException e) {
      log.fatal("Interrupted from sleep");
    }
  }

  
  /**
   * JSCH {@link SocketFactory} class that allows one to use an already
   * connected socket with JSCH.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  private class SshClientSocketFactory implements SocketFactory {
    
    private Socket connectedSocket;
    
    /**
     * Used to create a {@link SocketFactory} with an already connected
     * socket.
     * 
     * @param s and already connected socket.
     */
    public SshClientSocketFactory(Socket s) {
      this.connectedSocket = s;
    }
    
    /**
     * Returns the socket this factory was constructed with.
     * 
     * @param host no-op only to satisfy wonky interface
     * @param port no-op only to satisfy wonky interface
     */
    public Socket createSocket(String host, int port) {
      return connectedSocket;
    }
    
    /**
     * Calls the supplied socket's getInputStream() method.  This is a 
     * no-op to satisfy interface requirement.
     * 
     * @param s the socket return an InputStream from.
     */
    public InputStream getInputStream(Socket s) throws IOException {
      return s.getInputStream();
    }

    /**
     * Calls the supplied socket's getOutputStream() method.  This is a 
     * no-op to satisfy interface requirement.
     * 
     * @param s the socket return an OutputStream from.
     */
    public OutputStream getOutputStream(Socket s) throws IOException {
      return s.getOutputStream();
    }
  }
 
  
  /**
   * Provides fixed tunnel-service host key to JSCH to verify tunnel-server.
   * 
   * @author rayc@google.com (Ray Colline)
   *
   */
  public class TestClientKnownHosts implements HostKeyRepository {
    
    // key ID for tunnel server.
    private String KNOWN_HOSTS_ID = "Tunnel Server";
    
    // the hostkey.
    private HostKey hostKey;
    
    /**
     * Creates the repository with the tunnel server key inserted.
     * 
     * @throws JSchException if key cannot be parsed.
     */
    public TestClientKnownHosts() throws JSchException {
      hostKey = new HostKey("localhost", HostKey.SSHRSA, 
          serverHostKey.getBytes());
    }
    
    /**
     * No-op to fit implementation.
     */
    public void add(HostKey hostkey, UserInfo ui) {
      //pass
    }
    
    /**
     * Checks the provided hostkey to verify its the tunnel-service key. 
     * 
     * TODO(rayc) use this instead of setting strictHostKeyChecking=no.
     * 
     * @return the result of the check.  See {@link HostKey} for constants'
     *             meaning.
     */
    public int check(String host, byte[] key) {
      String keyString = new String(key);
      if (hostKey.getHost().equalsIgnoreCase(host)) {
        if (hostKey.getKey().equals(keyString)) {
          return HostKeyRepository.OK;
        } else {
          return HostKeyRepository.CHANGED;
        }
      }
      return HostKeyRepository.NOT_INCLUDED;
    }
    
    /**
     * Returns the server host key.
     */
    public HostKey[] getHostKey() {
      HostKey[] keys = new HostKey[1];
      keys[0] = hostKey;
      return keys;
    }

    /**
     * No-op that just returns the server hostkey. 
     */
    public HostKey[] getHostKey(String host, String type) {
      return getHostKey();
    }
    
    public String getKnownHostsRepositoryID() {
      return KNOWN_HOSTS_ID;
    }

    /**
     * No-op for implementation
     */
    public void remove(String host, String type) {
      //pass
    }
    
    /**
     * No-op for implementation
     */
    public void remove(String host, String type, byte[] key) {
      //pass
    }
  }
  
  
  /**
   * JSCH Logger implementation that passes all logs through to 
   * SshClient's logs.
   * 
   * @author rayc@google.com (Ray Colline)
   *
   */
  public class TestSshClientLogger implements Logger {
    
    public void log (int level, String message) {
      switch (level) {
        case Logger.DEBUG:
          log.info(message);
          break;
        case Logger.INFO:
          log.info(message);
          break;
        case Logger.ERROR:
          log.info(message);
          break;
        case Logger.FATAL:
          log.info(message);
          break;
        default:
          log.info(message);
      }
    }
    
    /**
     * No-op as all logging is enabled by default.
     */
    public boolean isEnabled(int level) {
      return true;
    }
  }
}
