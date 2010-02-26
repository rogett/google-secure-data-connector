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

import com.google.dataconnector.client.HealthCheckHandler.FailCallback;
import com.google.dataconnector.protocol.FrameReceiver;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.AuthorizationInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.SSLSocketFactoryInit;
import com.google.dataconnector.util.ShutdownManager;
import com.google.dataconnector.util.Stoppable;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;

/**
 * Implements a Secure Data Connector client.  Connects to Secure Data Connector Server, authorizes,
 * registers and sets up socket forwarding over the existing channel.
 *
 * @author rayc@google.com (Ray Colline)
 * @author vnori@google.com (Vasu Nori)
 */
public class SdcConnection implements FailCallback, Stoppable {

  // Logging instance
  private static final Logger LOG = Logger.getLogger(SdcConnection.class);

  public static final Integer DEFAULT_SOCKS_PORT = 1080;

  // TODO(rayc) Add in 256 cipher support if policy jars allow.
  private static final String[] SECURE_CIPHER_SUITE = {
    "TLS_RSA_WITH_AES_128_CBC_SHA"
  };

  public static final String INITIAL_HANDSHAKE_MSG = "v5.0 " +
     SdcConnection.class.getPackage().getImplementationVersion() + "\n";

  // Dependencies.
  private final LocalConf localConf;
  private final SSLSocketFactoryInit sslSocketFactoryInit;
  private final FrameReceiver frameReceiver;
  private final FrameSender frameSender;
  private final Registration registration;
  private final SocksDataHandler socksDataHandler;
  private final HealthCheckHandler healthCheckHandler;
  private final FetchRequestHandler agentRequestHandler;
  private final ResourcesFileWatcher resourcesFileWatcher;
  private final ShutdownManager shutdownManager;

  // Fields
  private SSLSocket socket;

 /**
  *  Sets up a Secure Data connection to a Secure Link server with the supplied configuration.
  *
  * @param localConf
  * @param sslSocketFactoryInit
  * @param frameReceiver
  * @param frameSender
  * @param registration
  * @param socksDataHandler
  * @param healthCheckHandler
  * @param agentRequestHandler
  * @param resourcesFileWatcher
  * @param shutdownManager
  */
  @Inject
  public SdcConnection(final LocalConf localConf,
      final SSLSocketFactoryInit sslSocketFactoryInit,
      final FrameReceiver frameReceiver,
      final FrameSender frameSender,
      final Registration registration,
      final SocksDataHandler socksDataHandler,
      final HealthCheckHandler healthCheckHandler,
      final FetchRequestHandler agentRequestHandler,
      final ResourcesFileWatcher resourcesFileWatcher,
      final ShutdownManager shutdownManager) {
    this.localConf = localConf;
    this.sslSocketFactoryInit = sslSocketFactoryInit;
    this.frameReceiver = frameReceiver;
    this.frameSender = frameSender;
    this.registration = registration;
    this.socksDataHandler = socksDataHandler;
    this.healthCheckHandler = healthCheckHandler;
    this.agentRequestHandler = agentRequestHandler;
    this.resourcesFileWatcher = resourcesFileWatcher;
    this.shutdownManager = shutdownManager;
  }

  /**
   * Connects to the tunnel-server and spawns SSHD on the open socket. The sshd
   * is run in inetd mode (sshd -i) and its stdin and stdout are connected
   * to the established socket.
   *
   * @throws ConnectionException if and error occurs with authorization or registration.
   */
  public void connect() throws ConnectionException {
    LOG.info("Connecting to SDC server");

    try {
      // Set runtime dependency.
      // TODO(rayc) figure out a cooler way to do this.
      registration.setHealthCheckHandler(healthCheckHandler);
      
      // Setup SSL connection and verify.
      LOG.debug("setting up SSLSocket with customized SSLSocketFacory");
      final SSLSocketFactory sslSocketFactory = sslSocketFactoryInit
          .getSslSocketFactory(localConf);
      socket = (SSLSocket) sslSocketFactory.createSocket();
      socket.setEnabledCipherSuites(SECURE_CIPHER_SUITE);
      // wait for 30 sec to connect. is that too long?
      socket.connect(new InetSocketAddress(localConf.getSdcServerHost(),
          localConf.getSdcServerPort()), 30 *1000);

      if (!localConf.getAllowUnverifiedCertificates()) {
        verifySubjectInCertificate(socket.getSession());
      }

      // send a message to initiate handshake with tunnelserver
      LOG.info("Sending initial handshake msg");
      final byte[] handshake = INITIAL_HANDSHAKE_MSG.getBytes();
      socket.getOutputStream().write(handshake);
      socket.getOutputStream().flush();

      // setup frame IO
      frameReceiver.setInputStream(socket.getInputStream());
      frameSender.setOutputStream(socket.getOutputStream());
      frameSender.start();

      LOG.info("Attemping login");

      if (!authorize()) {
        throw new ConnectionException("Authorization failed");
      }
      LOG.info("Successful login");

      // send registration info to the SDC server
      registration.sendRegistrationInfo(frameSender);

      // setup to start receiving and processing registration response
      frameReceiver.registerDispatcher(FrameInfo.Type.REGISTRATION, registration);

      // Setup Healthcheck
      healthCheckHandler.setFrameSender(frameSender);
      healthCheckHandler.setFailCallback(this);
      frameReceiver.registerDispatcher(FrameInfo.Type.HEALTH_CHECK, healthCheckHandler);
      healthCheckHandler.start();

      // Setup Socket Data.
      socksDataHandler.setFrameSender(frameSender);
      frameReceiver.registerDispatcher(FrameInfo.Type.SOCKET_DATA, socksDataHandler);

      // Setup AgentRequest handler
      agentRequestHandler.setFrameSender(frameSender);
      frameReceiver.registerDispatcher(FrameInfo.Type.FETCH_REQUEST, agentRequestHandler);
      
      // a thread to watch for changes in the resources.xml file
      // make this thread a daemon - so it can't hold up the process from exiting
      LOG.info("starting a thread to watch resources file");
      resourcesFileWatcher.setFrameSender(frameSender);
      resourcesFileWatcher.start();

      // Add to shutdown manager so it gets gracefully shutdown.
      shutdownManager.addStoppable(this);
      frameReceiver.startDispatching();
    } catch (IOException e) {
      throw new ConnectionException(e);
    } catch (FramingException e) {
      throw new ConnectionException(e);
    }
  }
  
  /** 
   * Kills active SDC connection and cleans up resources.
   */
  @Override
  public void shutdown() {
    try {
      // should cause frame receiver to exit its loop as the read call will throw an IOException.
      socket.close();
    } catch (IOException e) {
      LOG.debug("Socket exception when closing.", e);
    } 
  }

  /**
   * Creates authorization request and sends to server and awaits response.
   *
   * @returns true if successfully logged on or false otherwise.
   */
  boolean authorize() {
    try {
      // Authenticate
      final AuthorizationInfo authInfoRequest = AuthorizationInfo.newBuilder()
          .setEmail(localConf.getUser() + "@" + localConf.getDomain())
          .setPassword(localConf.getPassword())
          .build();
      final FrameInfo authReqRawFrame = FrameInfo.newBuilder()
          .setPayload(authInfoRequest.toByteString())
          .setType(FrameInfo.Type.AUTHORIZATION)
          .build();
      frameSender.sendFrame(authReqRawFrame);
      final FrameInfo authRespRawFrame = frameReceiver.readOneFrame();
      final AuthorizationInfo authInfoResponse = AuthorizationInfo.parseFrom(
          authRespRawFrame.getPayload());
      if (authInfoResponse.getResult() != AuthorizationInfo.ResultCode.OK) {
        LOG.error("Auth Result: " + authInfoResponse.getResult().toString());
        LOG.error("Auth Error Message: " + authInfoResponse.getStatusMessage().toString());
        return false;
      }
      return true;
    } catch (FramingException e) {
      LOG.warn("Frame error", e);
      return false;
    } catch (InvalidProtocolBufferException e) {
      LOG.warn("AuthInfo protocol parse error", e);
      return false;
    }
  }

  /**
   * Verifies that the server certificate has the correct Subject DN name.
   * Throws an exception otherwise, since the server may be impersonating a
   * legitimate Tunnel Server.
   *
   * @throws ConnectionException if server name does not match, DN is un-parseable or the
   * SSLSession does not have a peer certificate.
   */
  void verifySubjectInCertificate(final SSLSession session)
      throws ConnectionException {

    // Get Principal from session.
    final X509Certificate cert;
    try {
      cert = session.getPeerCertificateChain()[0];
    } catch (SSLPeerUnverifiedException e) {
      throw new ConnectionException(e);
    }
    Principal principal = cert.getSubjectDN();

    // Compare CNs between actual host and the one we thought we connected to.
    final Rdn expectedCn;
    try {
      expectedCn = new Rdn("CN", localConf.getSdcServerHost());

      // Get actual CN
      final LdapName ldapName = new LdapName(principal.getName());
      Rdn actualCn = null;
      for (final Rdn rdn : ldapName.getRdns()) {
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
      final String errorMessage = "Wrong server X.500 name. Expected: <" +
        localConf.getSdcServerHost() + ">. Actual: <" +
        (actualCn == null ? "null" : actualCn.getValue()) + ">.";
      LOG.error(errorMessage);
      throw new ConnectionException(errorMessage);

    } catch (InvalidNameException e) {
      throw new ConnectionException(e);
    }
  }

  /**
   * Closes underlying socket for this SDC connection.  Which shuts down the SDC agent.
   */
  @Override
  public void handleFailure() {
    LOG.error("Closing SDC connection due to health check failure.");
    // Will cause connect() to unblock.
    this.shutdown();
  }
  
  public boolean hasConnectedSuccessfully() {
    return healthCheckHandler.hasHadAtleastOneSuccessfulHealthCheck();
  }
  
}
