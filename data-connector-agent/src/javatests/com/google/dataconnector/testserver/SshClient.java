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

package com.google.dataconnector.testserver;

import com.google.dataconnector.util.ConnectionException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ServerSocketFactory;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SshClient identical to the one used by the server in the cloud.
 * This is used by the TestServer to simulate a server for the client to talk to, for
 * testing purposes.
 * 
 * @author vnori@google.com (Vasu Nori)
 */
public class SshClient {
  private static final Logger LOG = Logger.getLogger(SshClient.class);

  private JSch jsch = new JSch();
  private Session jschSession;
  private TestServerSocketFactory serverSocketFactory = new TestServerSocketFactory();

  /**
   * the public constructor used by the testserver to create a new instance of SshClient
   * 
   * @param sshPrivateKeyFile identical to the one used by the server in the cloud
   * @throws ConnectionException thrown if there is a problem with instantiation of SshClient.
   */
  public SshClient(String sshPrivateKeyFile) throws ConnectionException {
        
    /* Add the private key we used to communicate to the server.  This is one way we ensure
     * that the Client is really talking to Google.
     */
    LOG.info("Loading SSH private key file from " + sshPrivateKeyFile);
    try {
      jsch.addIdentity(sshPrivateKeyFile);

      /* We dont use SSH auth for this connection as we have our own Woodstock protocol 
       * authorization.
       */
      jschSession = jsch.getSession("woodstock", "", 0);
    } catch (JSchException e) {
      throw new ConnectionException(e);
    }    
    
    /* JSCH requires cryptography to authenticate but it can be turned off
     * after successful connection    
     */
    jschSession.setConfig("cipher.s2c", "blowfish-cbc");
    jschSession.setConfig("cipher.c2s", "blowfish-cbc");
    
    /* We disable this by default since, the host key is easy obtainable through downloading
     * the source from code.google.com and any hacker could easily just present this key.
     */
    jschSession.setConfig("StrictHostKeyChecking", "no");
  }
  
  public void connect(Socket socket, int localForwardPort, int remoteSocksPort) 
      throws ConnectionException {

    /* 
     * Creates the factory that will reuse the existing socket provided to us for the ssh
     * connection.  We set the server counters used for total server bytes and per domain.  Each 
     * SSH connection is associated with a given client which is owned by a domain.
     */
    SshClientSocketFactory scsf = new SshClientSocketFactory(socket); 
    jschSession.setSocketFactory(scsf);
    
    try {
      jschSession.connect();
  
      /*
       * Set cipher to none now that we have authenticated. Encryption is handled by the underlying 
       * TLS
       */
      LOG.debug("Setting cipher to 'none'");
      jschSession.setConfig("cipher.s2c", "None");
      jschSession.setConfig("cipher.c2s", "None");
     
      /*
       * Setup port forwarding using SSH from a local port to the Secure Link client's 
       * SOCKS server.  This is port advertised to Google Applications wishing to traverse the 
       * firewall.
       */
      jschSession.setPortForwardingL("0.0.0.0", localForwardPort,  "localhost", remoteSocksPort, 
          serverSocketFactory);
      LOG.info("Remote SOCKS port of client is " + remoteSocksPort);
      LOG.info("Connected to woodstock agent's SSHD");
    } catch (JSchException e) {
      throw new ConnectionException(e);
    }
  }

  public void close() {
    jschSession.disconnect();
  }
  
  public class TestServerSocketFactory implements ServerSocketFactory {
    public ServerSocket createServerSocket(int port, int backlog, 
        InetAddress bindAddr) throws IOException {
       return new ServerSocket(port, backlog, bindAddr);
    }
  }

  class SshClientSocketFactory implements SocketFactory {
    
    private Socket connectedSocket;
    
    /**
     * Used to create a {@link SocketFactory} with an already connected
     * socket.
     * 
     * @param s an already connected socket.
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

    public InputStream getInputStream(Socket socket) throws IOException {
      return socket.getInputStream();
    }

    public OutputStream getOutputStream(Socket socket) throws IOException {
      return socket.getOutputStream();
    }
  }
}
