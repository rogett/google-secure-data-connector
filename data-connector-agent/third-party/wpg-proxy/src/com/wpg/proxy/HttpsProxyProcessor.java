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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class is a main processor https requests. It is very similar to the
 * ProxyProcessor class and differs from it in the way networking is done. This
 * class uses older blocking IO sockets whereas ProxyProcessor class is based on
 * NIO. This was a temporary solution to get Https fixed. TODO: Get it
 * right. This class may become redundant if we refactor Https processing to use
 * NIO. At the very least it should be renamed to reflect its true nature
 * 
 */

public class HttpsProxyProcessor {

  /** Default port for an SSL connection. */
  public static final int DEFAULT_SSL_PORT = 443;

  /** Configuration object for the proxy. */
  private ProxyRegistry proxyRegistry;
  /** The port the proxy runs on. */

  private int port;

  /** Configuration for the KeyStore. */
  private KeyStoreConfig keyStoreConfig;

  /** The size of the buffer. */
  private static final int BUFFER_SIZE = 1024 * 1024 * 10;

  /** HTTP 200 message sent to the client after establishing connection. */
  private static final String CONNECTION_ESTABLISHED_HTTP_200 =
      "HTTP/1.0 200 Connection established\r\n "
          + "Proxy-agent: WPG-Proxy/1.0\r\n\r\n";

  /** Logger object used for all logging activities. */
  private static Logger logger = Logger.getLogger(HttpsProxyProcessor.class);

  /**
   * Creates a new instance of HttpsProxyProcessor.
   * 
   * @param socket - connected to the client
   * @param registry - configuration for the proxy
   * @param proxyPort - port to run the proxy on
   * @param ksc - configuration for the KeyStore
   * @throws IOException network exception
   */
  public HttpsProxyProcessor(final Socket socket, final ProxyRegistry registry,
      final int proxyPort, final KeyStoreConfig ksc) throws IOException {
    this.proxyRegistry = registry;
    this.port = proxyPort;
    this.keyStoreConfig = ksc;
    processConnection(socket);
  }

  /**
   * Executes the specified executor on a request.
   * 
   * @param executor - executor to run
   * @param request - request object to apply executor at
   * @return response from the executor
   */
  private HttpMessageResponse runMessageExecutor(
      final HttpMessageExecutor executor, final HttpMessageRequest request) {
    if (executor == null) {
      logger.info("Message executor has not been set");
      return null;
    }
    return executor.executeRequest(request);
  }

  /**
   * Run registered HttpMessageHandlers for a given exception.
   * 
   * @param handlers - collection of handlers to run
   * @param e - exception to process by handlers
   */
  private void runHandlers(final Vector<HttpMessageHandler> handlers,
      final Exception e) {
    runHandlers(handlers, null, null, e);
  }

  /**
   * Run registered HttpMessageHandlers on a HttpMessage.
   * 
   * @param handlers - collection of handlers to run
   * @param request - request object to apply handlers to
   * @param response - response object received from the target server
   * @param e - exception to process
   */
  private void runHandlers(final Vector<HttpMessageHandler> handlers,
      final HttpMessageRequest request, final HttpMessageResponse response,
      final Exception e) {
    if (handlers.isEmpty()) {
      logger.info("No handlers registered, continuing");
      return;
    }
    int count = 1;
    for (HttpMessageHandler hml : handlers) {
      logger.trace("Processing Request Handler " + (count++) + " of "
          + handlers.size());
      if (response != null && request != null && e != null) {
        hml.failedResponse(response, request, e);
      } else if (request != null && e != null) {
        hml.failedRequest(request, e);
      } else if (e != null) {
        hml.failed(e);
      } else if (response != null && request != null) {
        hml.receivedResponse(response, request);
      } else if (request != null) {
        hml.receivedRequest(request);
      }
    }
  }

  /**
   * Prepares the response to be send back to the client.
   * 
   * @param response Response object to be sent back to client
   * @return ByteBuffer containing response bytes
   */
  private ByteBuffer processResponse(final HttpMessageResponse response) {
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    buffer.clear();
    if (response.getStartLine() != null) {
      buffer.put(response.getStartLine().getBytes());
    }
    buffer.put((byte) '\r');
    buffer.put((byte) '\n');
    buffer.put(response.getHeadersAsString().getBytes());
    buffer.put((byte) '\r');
    buffer.put((byte) '\n');
    if (response.getBodyContent() != null) {
      if (!response.isContentLengthSet()) {
        buffer.put(new Integer(response.getBodyContent().length).toString()
            .getBytes());
        buffer.put((byte) '\r');
        buffer.put((byte) '\n');
      }
      buffer.put(response.getBodyContent());
    }
    buffer.flip();
    return buffer;
  }

  /**
   * Ends the transaction and creates stats for it.
   * 
   * @param startTimeStamp beginning of the transaction
   * @param status - transaction status
   */
  private void stopTransaction(final long startTimeStamp, final int status) {
    long endTimeStamp = System.currentTimeMillis();
    long duration = endTimeStamp - startTimeStamp;
    logger.trace("Duration: " + duration);
    ProxyStatistics.getSingleton().addDuration(duration / 1000);
    ProxyStatistics.getSingleton().incrementTransactionCount(status);
  }

  /**
   * Main processing logic for the https tunneling proxy. It only acts on a HTTP
   * CONNECT request that indicated the beginning of the SSL session. It
   * connects to the target host, sends back to the client an acknowledgment
   * message and spawns 2 processing threads each reads on one end of the
   * conversation and writes on the other.
   * 
   * @param client - Socket connected to the client
   * @throws IOException - network IO problem
   */
  private void processConnection(final Socket client) throws IOException {
    long startTimeStamp = System.currentTimeMillis();
    HttpMessageRequest request = null;
    try {
      request = parseRequest(client);
    } catch (IOException e) {
      stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
      logger.error("Exception while parsing the request: " + e, e);
      runHandlers(proxyRegistry.getHandlers(), e);
      client.close();
      return;
    } catch (URISyntaxException e) {
      stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
      logger.error("Exception while parsing the request URL: " + e, e);
      runHandlers(proxyRegistry.getHandlers(), e);
      client.close();
      return;
    }
    if (request.getMethod().equals("CONNECT")) {
      logger.info("Switching this connection to SSL");
      logger.trace("CONNECT method found, sending reply");
      HttpMessageResponse response =
          runMessageExecutor(proxyRegistry.getMessageExecutor(), request);
      if (response != null) {
        // Response should be sent back to the client without
        // contacting the target host
        ByteBuffer buf = processResponse(response);
        OutputStream outStream = client.getOutputStream();
        try {
          outStream.write(buf.array());
          outStream.flush();
          outStream.close();
        } catch (IOException ex) {
          client.close();
        }
        return;
      }
      byte[] msg = CONNECTION_ESTABLISHED_HTTP_200.getBytes();
      OutputStream outStream = client.getOutputStream();
      outStream.write(msg);
      outStream.flush();
      int targetPort = request.getToPort();
      if (targetPort < 0) {
        targetPort = DEFAULT_SSL_PORT;
      }
      String targetHost = request.getToHost();
      new SSLServer(port, targetHost, targetPort, client, proxyRegistry,
          keyStoreConfig).start();
    }
  }

  /**
   * Reads response from the socket and packages it into Request object.
   * 
   * @param socket - connected to the client
   * @return HttpMessageRequest object
   * @throws IOException - network exception
   * @throws URISyntaxException - URI is malformed
   */
  private HttpMessageRequest parseRequest(final Socket socket)
      throws IOException, URISyntaxException {
    HttpMessageRequest request = new HttpMessageRequest();
    request.setFromHost(socket.getLocalAddress().getHostAddress());
    request.setFromPort(socket.getLocalPort());
    InputStream reader = socket.getInputStream();
    BufferedReader is = new BufferedReader(new InputStreamReader(reader));
    String startLine = is.readLine();
    logger.info("Request: " + startLine);
    request.setStartLine(startLine);
    StringTokenizer st = new StringTokenizer(startLine, " ");
    request.setMethod(st.nextToken());
    request.setUri(st.nextToken());

    st = new StringTokenizer(st.nextToken(), "/");
    request.setProtocol(st.nextToken());
    request.setVersion(st.nextToken());

    String line = null;
    Vector<String> headers = new Vector<String>();
    while ((line = is.readLine()).length() > 0) {
      headers.addElement(line.replaceAll("[\r\n]+", ""));
      logger.trace("request line: \"" + line + "\"");
      if (line.endsWith("\r\n\r\n")) {
        break;
      }
    }
    request.setHeaders(headers);
    logger.trace("Finished Reading Header of Request");
    char c;
    StringBuffer sb = new StringBuffer();
    while (is.ready()) {
      c = (char) is.read();
      sb.append(c);
    }
    if (sb.toString().getBytes().length > 0) {
      request.addToBody(sb.toString().getBytes(),
          sb.toString().getBytes().length);
    }
    logger.trace("Finished Reading Body of Request");
    return request;
  }
}
