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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Principal;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;

/**
 * Class that runs as local server and used for handling SSL connection in a
 * Man-in-the-Middle fashion. It receives a certificate from the target SSL site
 * and reissues this certificate as a new Certificate Authority. Doing that it
 * is able to have handle unencrypted conversation with the target SSL server.
 * 
 */
public class SSLServer extends Thread {

  /** The port local server runs on. */
  private int localPort;

  /** The target server host. */
  private String targetHost = "";

  /** The target server port. */
  private int targetPort;

  /** Socket connected to the client. */
  private Socket clientSocket = null;

  /** The local server socket. */
  private SSLServerSocket serverSocket = null;

  /** socket connected to target server. */
  private SSLSocket targetSocket = null;

  /** running flag. */
  private boolean runnning = false;

  /** configuration for the KeyStore. */
  private KeyStoreConfig keyStoreConfig;

  /** Logger object used for all logging activities. */
  private static Logger logger = Logger.getLogger(SSLServer.class);

  /**
   * Is server running?
   * 
   * @return true or false
   */
  public final boolean isRunning() {
    return runnning;
  }

  /**
   * @param runFlag - true if sever is running
   */
  public final void setRunning(final boolean runFlag) {
    runnning = runFlag;
  }

  /**
   * This class implements SSL Man-in-the-Middle server. It establishes a SSL
   * session with the target host, starts a local SSL server, connects local
   * client to local server and pipes data between client and local client and
   * local server and target. The chain of connections looks like the following:
   * 
   * client <---> localClient <---> localServer <---> target
   * 
   * local server sees unencrypted conversation between client and target,
   * target's certificate is received by the local server, then this certificate
   * is substituted by the new certificate issued locally by the local server
   * and this substituted certificate is sent to the client. Client will see
   * that certificate is valid, but CA is unknown. If client accepts this
   * certificate either on a one-time basis, or installs a new CA (server's
   * certificate which has been obtained from the supplied keystore) the whole
   * conversation will be functional.
   * 
   * @param localSecurePort - port to start a local server on
   * @param host - target server host
   * @param port - target server port
   * @param socket - connected to the client
   * @param proxyRegistry - configuration for the proxy
   * @param ksc - configuration for the KeyStore
   */
  public SSLServer(final int localSecurePort, final String host,
      final int port, final Socket socket, final ProxyRegistry proxyRegistry,
      final KeyStoreConfig ksc) {
    super("SSLServer");
    localPort = localSecurePort;
    targetHost = host;
    this.targetPort = port;
    clientSocket = socket;
    keyStoreConfig = ksc;
    initializeSSL();
  }

  /**
   * Initializes both SSL sessions
   */
  private void initializeSSL() {
    CertificateManager certManager = null;
    try {
      certManager = new CertificateManager(keyStoreConfig);
      initializeTargetSession(certManager);
      initializeClientSession(certManager);
      logger.debug("Finished SSL Init");
    } catch (IOException e) {
      logger.error("Error Initializing SSL: " + e, e);
    } catch (GeneralSecurityException e) {
      logger.error("Error Initializing SSL: " + e, e);
    }
  }

  /**
   * Initializes the SSL session with the target.
   * 
   * @param certManager - Certificate manager used
   * @throws IOException SLL initialization fails
   */
  private void initializeTargetSession(CertificateManager certManager)
      throws IOException {
    SSLContext targetSSLContext = certManager.getSSLContext();
    SSLSocketFactory socketFactory = targetSSLContext.getSocketFactory();
    targetSocket =
        (SSLSocket) socketFactory.createSocket(targetHost, targetPort);
    targetSocket.setUseClientMode(true);
    targetSocket
        .setEnabledCipherSuites(targetSocket.getSupportedCipherSuites());
    targetSocket.startHandshake();
  }

  /**
   * Initializes the SSL session with the client.
   * 
   * @param certManager - Certificate manager used
   * @throws IOException SLL initialization fails
   */
  private void initializeClientSession(CertificateManager certManager)
      throws IOException {
    X509Certificate[] certs =
        targetSocket.getSession().getPeerCertificateChain();
    Principal principal = certs[0].getSubjectDN();
    SSLContext localServerSSLContext =
        certManager.issueCertificate(principal.getName());
    SSLServerSocketFactory serverSocketFactory =
        localServerSSLContext.getServerSocketFactory();
    boolean connected = false;
    try {
      serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket();
      serverSocket.setReuseAddress(false);
    } catch (IOException ex) {
      logger.error(ex);
      return;
    }
    while (!connected) {
      try {
        InetSocketAddress addr = new InetSocketAddress("localhost", localPort);
        logger.debug("Trying to open local ssl server on port: " + localPort);
        serverSocket.bind(addr);
        connected = true;
      } catch (IOException e) {
        connected = false;
        localPort++;
      }
    }
    try {
      String[] supported = serverSocket.getSupportedCipherSuites();
      String[] anonSupported = new String[supported.length];
      int numAnonSupported = 0;
      for (int i = 0; i < supported.length; i++) {
        if (supported[i].indexOf("_anon_") > 0) {
          anonSupported[numAnonSupported++] = supported[i];
        }
      }
      String[] oldEnabled = serverSocket.getEnabledCipherSuites();
      String[] newEnabled = new String[oldEnabled.length + numAnonSupported];
      System.arraycopy(oldEnabled, 0, newEnabled, 0, oldEnabled.length);
      System.arraycopy(anonSupported, 0, newEnabled, oldEnabled.length,
          numAnonSupported);
      serverSocket.setEnabledCipherSuites(newEnabled);

    } catch (RuntimeException e) {
      logger.error("Error Initializing Server Socket: " + e, e);
    }
  }

  /**
   * Thread's run method. It creates local client socket and blocks until
   * connection with the local server is established. Then it starts 4 threads
   * for pumping data between client and localClient (both directions) and local
   * server and target (both directions). After it's done the everything is
   * ready for connecting client with the target.
   */
  @Override
  public final void run() {
    logger.info("Listening for connection");
    logger.debug("Starting ssl server thread "
        + Thread.currentThread().getName());
    try {
      LocalServer localServerThread = new LocalServer(serverSocket);
      localServerThread.start();
      logger.debug("Started local server");
      SocketFactory clientSocketFactory = SocketFactory.getDefault();
      Socket localClientSocket =
          clientSocketFactory.createSocket("localhost", localPort);
      logger.debug("Started local client - connection on port " + localPort);
      boolean localClientConnected = false;
      while (!localClientConnected) {
        try {
          localServerThread.join();
          localClientConnected = true;
        } catch (InterruptedException ex) {
          logger.error(ex);
        }
      }
      logger.debug("*******************Local client has connected");
      SSLSocket server = (SSLSocket) localServerThread.getSocket();
      if (server == null) {
        logger.warn("Exiting ssl server thread "
            + Thread.currentThread().getName());
        return;
      }
      ConnectStreams clientToLocalClient =
          new ConnectStreams(clientSocket, localClientSocket,
              "clientToLocalClient");
      ConnectStreams localClientToClient =
          new ConnectStreams(localClientSocket, clientSocket,
              "localClientToClient");
      ConnectStreams serverToTarget =
          new ConnectStreams(server, targetSocket, "serverToTarget");
      ConnectStreams targetToServer =
          new ConnectStreams(targetSocket, server, "targetToServer");
      clientToLocalClient.start();
      localClientToClient.start();
      serverToTarget.start();
      targetToServer.start();
      return;
    } catch (IOException e) {
      logger.error("IOException while starting IORedirector Threads: " + e, e);
      return;
    }
  }

  /**
   * Helper class to assist in connecting the local client to the local server
   * 
   */
  class LocalServer extends Thread {
    /** Local server socket. */
    private SSLServerSocket sSocket;
    /** Socket connected to the local client. */
    private Socket socket;

    /**
     * Constructs local server.
     * 
     * @param s - Server socket to listen to
     */
    public LocalServer(final SSLServerSocket s) {
      super("Local server for " + s);
      sSocket = s;
    }

    /** Thread's run method. */
    @Override
    public void run() {
      try {
        logger.debug("local server is listening at " + sSocket.getInetAddress()
            + ":" + sSocket.getLocalPort());
        socket = sSocket.accept();
        logger.debug("local server is connected at port "
            + sSocket.getLocalPort());
      } catch (IOException e) {
        logger.error("Socket Accept Exception: " + e, e);
      }
    }

    /**
     * @return connected socket
     */
    public Socket getSocket() {
      return socket;
    }
  }
}
