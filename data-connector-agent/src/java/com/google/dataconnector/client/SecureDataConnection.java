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

import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.util.ApacheSetupException;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Principal;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;

/**
 * Implements a Secure Data Connector client.  Connects to Secure Data Connector Server and spawns 
 * opensshd on the established socket.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class SecureDataConnection {

  // Logging instance
  private static final Logger log = Logger.getLogger(SecureDataConnection.class);

  public static final Integer DEFAULT_SOCKS_PORT = 1080;
  private static final String PERMIT_OPEN_OPT = "-o PermitOpen=";

  // TODO(rayc) Add in 256 cipher support if policy jars allow.
  private static final String[] SECURE_CIPHER_SUITE = {
    "TLS_RSA_WITH_AES_128_CBC_SHA"
  };

  // Injected dependencies. TODO(rayc) fully DI this code.
  /** Secure Data Connector Configuration */
  private LocalConf localConf;
  private List<ResourceRule> resourceRules;
  private SSLSocketFactory sslSocketFactory;
  private ClientRegistrationUtil clientRegistrationUtil;
  private JsocksStarter jsocksStarter;
  private ApacheStarter apacheStarter;

  /**
   * Sets up a Secure Data connection to a Secure Link server with the supplied configuration.
   *
   * @param localConf the local configuration object.
   * @param resourceRules the resource rule set.
   * @param sslSocketFactory A configured SSLSocketFactory.
   */
  @Inject
  public SecureDataConnection(LocalConf localConf, List<ResourceRule> resourceRules,
      SSLSocketFactory sslSocketFactory, ClientRegistrationUtil clientRegistrationUtil,
      ApacheStarter apacheStarter, JsocksStarter jsocksStarter) {
    this.localConf = localConf;
    this.resourceRules = resourceRules;
    this.sslSocketFactory = sslSocketFactory;
    this.clientRegistrationUtil = clientRegistrationUtil;
    this.apacheStarter = apacheStarter;
    this.jsocksStarter = jsocksStarter;
  }

  /**
   * Connects to the tunnel-server and spawns SSHD on the open socket. The sshd
   * is run in inetd mode (sshd -i) and its stdin and stdout are connected
   * to the established socket.
   *
   * @throws ConnectionException if and error occurs with authorization or registration.
   * @throws IOException if any socket communication errors occur with the Secure Link server.
   * @throws ApacheSetupException if apache has any errors starting up.
   */
  public void connect() throws IOException, ConnectionException, ApacheSetupException {
    log.info("Connecting to server");

    SSLSocket clientSocket = (SSLSocket) sslSocketFactory.createSocket();
    clientSocket.setEnabledCipherSuites(SECURE_CIPHER_SUITE);
    // wait for 30 sec to connect. is that too long?
    try {
      clientSocket.connect(new InetSocketAddress(localConf.getSdcServerHost(),
          localConf.getSdcServerPort()), 30 *1000);
      
      if (!localConf.getAllowUnverifiedCertificates()) {
        verifySubjectInCertificate(clientSocket.getSession());
      }
      
    } catch (SocketTimeoutException e) {
      throw new ConnectionException(e);
    }

    // login to SDC server
    AuthRequest authRequest = clientRegistrationUtil.authorize(clientSocket, localConf);
    
    // register the resource rules
    clientRegistrationUtil.register(clientSocket, authRequest,  resourceRules);

    // start apache and jsocks
    apacheStarter.startApacheHttpd();
    jsocksStarter.startJsocksProxy();

    /*
     * startup is successful. lets start the port forwarding using SSH.
     *
     * SSHD is run in inetd mode which results in the SSH protocol being
     * transmitted on its stdin and stdout.
     */
    log.info("Starting SSHD");
    
    // We add something like -oPermitOpen=127.0.0.1:1080 to the SSHD to restrict portforwards
    // to only ourselves.  Otherwise the remote side could forward any where they want.
    String startSshdArgs =  (localConf.getDebug() ? "debug " : " ") + 
        PERMIT_OPEN_OPT + "127.0.0.1" + ":" + localConf.getSocksServerPort();
    log.debug("startSshdArgs command: " + startSshdArgs);

    
    // Add PermitOpen to SSHD exectuable to restrict portforwards based on configuration.
    log.debug("Executing sshd: " + localConf.getSshd() + " " + startSshdArgs);
    Process sshdProcess = Runtime.getRuntime().exec(localConf.getSshd() + " " + startSshdArgs);
    if (sshdProcess == null) {
      // couldn't start openssh? not good
      throw new ConnectionException("couldn't start SSH @ " + localConf.getSshd() + ". exiting");
    }

    // SSH started ok. make sure it is killed when this VM exits
    Runtime.getRuntime().addShutdownHook(new KillProcessShutdownHook(sshdProcess));
    
    log.info("Connecting SSHD to existing stream");
    ConnectStreams connectSshdOutput = new ConnectStreams(
        sshdProcess.getInputStream(), clientSocket.getOutputStream(), "sshdOut");
    ConnectStreams connectSshdInput = new ConnectStreams(
        clientSocket.getInputStream(), sshdProcess.getOutputStream(), "sshdIn");
    connectSshdInput.start();
    connectSshdOutput.start();

    log.info("Last connection initialization step.");
    /*
     * OpenSSHD logs to stderr, so we pick up its log messages and log them
     * as our own.  This will stay active the entire length of the Secure Link Connection
     * as our transport is openssh.  NOTE that this thread will run forever.
     */
    new RedirectStreamToLog(sshdProcess.getErrorStream(), log, "OpenSSH Logline").start();
  }

  /**
   * Verifies that the server certificate has the correct Subject DN name.
   * Throws an exception otherwise, since the server may be impersonating a 
   * legitimate Tunnel Server.
   * 
   * @throws ConnectionException if server name does not match, DN is un-parseable or the 
   * SSLSession does not have a peer certificate.
   */
  void verifySubjectInCertificate(SSLSession session)
      throws ConnectionException {

    // Get Principal from session.
    X509Certificate cert;
    try {
      cert = session.getPeerCertificateChain()[0];
    } catch (SSLPeerUnverifiedException e) {
      throw new ConnectionException(e);
    }
    Principal principal = cert.getSubjectDN();

    // Compare CNs between actual host and the one we thought we connected to.
    Rdn expectedCn;
    try {
      expectedCn = new Rdn("CN", localConf.getSdcServerHost());

      // Get actual CN
      LdapName ldapName = new LdapName(principal.getName());
      Rdn actualCn = null;
      for (Rdn rdn : ldapName.getRdns()) {
        if (rdn.getType().equals("CN")) {
          actualCn = rdn;
          break;
        }
      }
      // Reported CN must match expected.
      if (expectedCn.equals(actualCn)) {
        return;
      }

      // No match, FAIL.
      String errorMessage = "Wrong server X.500 name. Expected: <" +
        localConf.getSdcServerHost() + ">. Actual: <" + 
        (actualCn == null ? "null" : actualCn.getValue()) + ">."; 
      log.error(errorMessage);
      throw new ConnectionException(errorMessage);

    } catch (InvalidNameException e) {
      throw new ConnectionException(e);
    }
  }
  
  

  /**
   * A thread to take data from the given stream and write it to the logger instance provided.
   */
  public static class RedirectStreamToLog extends Thread {
    private InputStream stream;
    private Logger logger;
    private String id;
    
    public RedirectStreamToLog(InputStream stream, Logger logger, String id) {
      this.stream = stream;
      this.logger = logger;
      this.id = id;
    }
    
    @Override
    public void run() {
      Thread.currentThread().setName(id);
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String lineFromStream;
        while ((lineFromStream = br.readLine()) != null) {
          logger.info(lineFromStream);
        }
      } catch (IOException e) {
        // stream may have been closed. exit this thread
        return;
      }
    }
  }

  /**
   * A shutdownhook to kill the given process when the VM exists
   *
   */
  public static class KillProcessShutdownHook extends Thread {
    private Process proc;
    
    public KillProcessShutdownHook(Process proc) {
      this.proc = proc;
    }
    
    @Override
    public void run() {
        proc.destroy();
    }
  }

  /**
   * Class that allocates a thread to move data from an {@link InputStream}
   * to an {@link OutputStream} in 64k blocks.
   *
   * @author rayc@google.com (Ray Colline)
   */
  private class ConnectStreams extends Thread {

    // Input stream to read from
    private InputStream in;

    // Output stream to write to.
    private OutputStream out;

    /**
     * Creates a {@link ConnectStreams} object.
     *
     * @param in stream to read from
     * @param out stream to write to.
     * @param id thread name for identification.
     */
    public ConnectStreams(final InputStream in, final OutputStream out, final String id) {
      this.in = in;
      this.out = out;
      this.setName(id);
      this.setDaemon(true);
    }

    /**
     * Reads up to 64k from the InputStream and writes it to the OutputStream.
     * This routine blocks when data is not available.
     */
    @Override
    public void run() {
      try {
        byte[] buffer = new byte[65536];
        while (true) {
          int bytesRead = in.read(buffer);
          /*
           * If -1 is returned, the peer has closed the connection and we should break out
           * of the loop closing down this thread.
           */
          if (bytesRead == -1) {
            break;
          }
          out.write(buffer, 0, bytesRead);
          log.debug(getName() + ":Wrote " + bytesRead + " bytes");
          out.flush();
        }
      } catch (SocketException e) {
        log.error("Socket Error", e);
      } catch (IOException e) {
        log.error("Client Error", e);
      }
      
      /* 
       * about to disconnect connection between client-TS socket AND the SSH. 
       * doesn't matter why we reached here - 
       * but this means client can no longer process requests from the TS. 
       */
      log.info("Communication interrupted between server and client, exiting.");
      System.exit(1);
    }
  }
}
