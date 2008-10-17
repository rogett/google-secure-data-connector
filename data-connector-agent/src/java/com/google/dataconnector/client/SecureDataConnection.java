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

import com.google.dataconnector.util.AuthRequest;
import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.ConnectionException;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Implements a Secure Link client.  Connects to Secure Link Server and spawns opensshd on the
 * established socket.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class SecureDataConnection {

  // Logging instance
  private static final Logger log = Logger.getLogger(SecureDataConnection.class);

  public static final Integer DEFAULT_SOCKS_PORT = 1080;

  // Injected dependencies. TODO(rayc) fully DI this code.
  private ClientConf clientConf;
  private SSLSocketFactory sslSocketFactory;

  /**
   * Sets up a Secure Link connection to a Secure Link server with the supplied configuration.
   *
   * @param clientConf A Secure Link Client configuration object.
   * @param sslSocketFactory A configured SSLSocketFactory.
   */
  public SecureDataConnection(final ClientConf clientConf,
      final SSLSocketFactory sslSocketFactory) {
    this.clientConf = clientConf;
    this.sslSocketFactory = sslSocketFactory;
  }

  /**
   * Connects to the tunnel-server and spawns SSHD on the open socket. The sshd
   * is run in inetd mode (sshd -i) and its stdin and stdout are connected
   * to the established socket.
   *
   * @throws ConnectionException if and error occurs with authorization or registration.
   * @throws IOException if any socket communication errors occur with the Secure Link server.
   */
  public void connect() throws IOException, ConnectionException {
    log.info("Connecting to server");

    Socket clientSocket;
    if (sslSocketFactory != null) { // We have an SSL socket factory, use it.
      clientSocket = sslSocketFactory.createSocket();
      // Enable all support cipher suites.
      SSLSocket sslClientSocketRef = (SSLSocket) clientSocket;
      sslClientSocketRef.setEnabledCipherSuites(sslClientSocketRef.getSupportedCipherSuites());
      clientSocket.connect(new InetSocketAddress(clientConf.getSecureLinkServerHost(),
          clientConf.getSecureLinkServerPort()));
    } else { // Fall back to straight TCP (testing only!)
      clientSocket = new Socket(clientConf.getSecureLinkServerHost(),
          clientConf.getSecureLinkServerPort());
    }

    // Attempt to login
    AuthRequest authRequest = ClientRegistrationUtil.authorize(clientSocket, clientConf);
    ClientRegistrationUtil.register(clientSocket, authRequest,  clientConf);
    log.info("Login successful as " + clientConf.getEmail());

    /*
     * Auth was successful lets start the port forwarding using SSH.
     *
     * SSHD is run in inetd mode which results in the SSH protocol being
     * transmitted on its stdin and stdout.
     */
    log.info("Starting SSHD");
    Process p = Runtime.getRuntime().exec(clientConf.getSshd());

    log.info("Connecting SSHD to existing stream");
    ConnectStreams connectSshdOutput = new ConnectStreams(
        p.getInputStream(), clientSocket.getOutputStream(), "sshdOut");
    ConnectStreams connectSshdInput = new ConnectStreams(
        clientSocket.getInputStream(), p.getOutputStream(), "sshdIn");
    connectSshdInput.start();
    connectSshdOutput.start();

    /*
     * OpenSSHD logs to stderr, so we pick up its log messages and log them
     * as our own.  This will stay active the entire length of the Secure Link Connection
     * as our transport is openssh.  When openssh goes down, this thread will exit.
     */
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    String foo;
    while ((foo = br.readLine()) != null) {
      log.info("OpenSSH Logline: " + foo);
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
    }

    /**
     * Reads up to 64k from the InputStream and writes it to the OutputStream.
     * This routine blocks when data is not available.
     */
    @Override
    public void run() {
      try {
        synchronized (in) {
          synchronized (out) {
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
          }
        }
      } catch (SocketException e) {
        log.error("Socket Error", e);
        System.exit(1);
      } catch (IOException e) {
        log.error("Client Error", e);
        System.exit(1);
      }
    }
  }
}
