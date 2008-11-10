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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author opsi
 */
public class ProxyProcessor {
  private InetAddress inetAddr = null;
  private Selector selector;
  private Selector secureSelector;
  private int port = 8080;
  private int securePort = 14111;
  
  /** The size of the buffer. */
  private static final int BUFFER_SIZE = 1024 * 1024 * 10;

  private ProxyRegistry proxyRegistry;

  private static Logger logger = Logger.getLogger(ProxyProcessor.class);

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

  /** Run registered HttpMessageHandlers on a HttpMessage */
  private void runHandlers(Vector<HttpMessageHandler> handlers, HttpMessageRequest request) {
    runHandlers(handlers, request, null, null);
  }

  /** Run registered HttpMessageHandlers on a HttpMessage */
  private void runHandlers(Vector<HttpMessageHandler> handlers, HttpMessageResponse response) {
    runHandlers(handlers, null, response, null);
  }

  /** Run registered HttpMessageHandlers on a HttpMessage */
  private void runHandlers(Vector<HttpMessageHandler> handlers, HttpMessageRequest request,
      Exception e) {
    runHandlers(handlers, request, null, e);
  }

  /** Run registered HttpMessageHandlers on a HttpMessage */
  private void runHandlers(Vector<HttpMessageHandler> handlers, HttpMessageResponse response,
      Exception e) {
    runHandlers(handlers, null, response, e);
  }

  /** Run registered HttpMessageHandlers on a HttpMessage */
  private void runHandlers(Vector<HttpMessageHandler> handlers, HttpMessageRequest request,
      HttpMessageResponse response) {
    runHandlers(handlers, request, response, null);
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
      logger.debug("Processing Request Handler " + (count++) + " of "
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
    logger.debug("Duration: " + duration);
    ProxyStatistics.getSingleton().addDuration((double) (duration / 1000));
    ProxyStatistics.getSingleton().incrementTransactionCount(status);
  }
  

  private void processConnection(SelectionKey key) throws IOException {
    long startTimeStamp = System.currentTimeMillis();
    if (key.isValid() && key.isAcceptable()) {
      logger.debug("Event found, isAcceptable");
      ServerSocketChannel server = (ServerSocketChannel) key.channel();
      SocketChannel client = server.accept();
      logger.debug("Accepted connection from: " + client);

      HttpMessageRequest request = null;
      try {
        request = parseRequest(client);
      } catch (Exception e) {
        stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
        logger.error("Exception while parsing the request: " + e, e);
        runHandlers(proxyRegistry.getHandlers(), e);
        client.close();
        return;
      }

      if (proxyRegistry.isStatusBrowserEnabled()
          && isLocalRequest(request.getToHost(), request.getToPort())) {
        processLocalRequest(request, client);
        return;
      }

      // TODO implement HTTP/1.1 RFC2817 TLS OPTIONS "Upgrade" request as well
      // as HTTP/1.0 CONNECT
      if (request.getMethod().equals("CONNECT")) {
        try {
          // TODO upgrade this socket to an SSL connection back to our internal
          // ssl server
          if (proxyRegistry.getKeystoreFilename() == null)
            throw new Exception("SSL Not Enabled on this proxy instance");
          else
            logger.info("Switching this connection to SSL");
        } catch (Exception e) {
          logger.error("Exception while establishing the SSL Layer: " + e, e);
          client.configureBlocking(false);
          SelectionKey clientKey =
              client.register(selector, SelectionKey.OP_WRITE);
          ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 10);
          buf.clear();
          String errorString = "HTTP/1.0 500 Error " + e;
          buf.put(errorString.getBytes());
          buf.put((byte) '\r');
          buf.put((byte) '\n');
          buf.put("Proxy-agent: WPG-RecordingProxy/1.0".getBytes());
          buf.put((byte) '\r');
          buf.put((byte) '\n');
          buf.put((byte) '\r');
          buf.put((byte) '\n');
          // TODO put a pretty html message here explaining the error in more
          // detail
          buf.flip();
          clientKey.attach(buf);
          return;
        }
        logger.debug("CONNECT method found, sending reply");
        
        SSLServerThread sslServerThread =
            new SSLServerThread(securePort, request.getToHost(), 443, client
                .socket());
        sslServerThread.start();
        return;
      }

      request =
          runRequestProcessors(proxyRegistry.getRequestProcessors(), request);
      if (request == null) {
        stopTransaction(startTimeStamp, ProxyStatistics.STOPPED);
        client.close();
        return;
      }


      runHandlers(proxyRegistry.getHandlers(), request);

      HttpMessageResponse response =
          runMessageExecutor(proxyRegistry.getMessageExecutor(), request);
      if (response == null) {
        try {
          response = executeRequest(request);
        } catch (Exception e) {
          stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
          logger.error("Exception while executing the request: " + e, e);
          runHandlers(proxyRegistry.getHandlers(), request, e);
          client.close();
          return;
        }
      }

      // run response processors after the final request handlers
      response =
          runResponseProcessors(proxyRegistry.getResponseProcessors(), response);
      if (response == null) {
        stopTransaction(startTimeStamp, ProxyStatistics.STOPPED);
        client.close();
        return;
      }

      // run response handlers after response processors
      runHandlers(proxyRegistry.getHandlers(), request, response);

      try {
        client.configureBlocking(false);
        SelectionKey clientKey =
            client.register(selector, SelectionKey.OP_WRITE);
        clientKey.attach(response);
      } catch (Exception e) {
        stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
        logger.error("Exception while returning the response: " + e, e);
        runHandlers(proxyRegistry.getHandlers(), request, response, e);
        client.close();
        return;
      }
      logger.debug("Finished Handler for request");
    }

    if (key.isValid() && key.isConnectable()) {
      logger.debug("Event found, isConnectable");
      SocketChannel sChannel = (SocketChannel) key.channel();
      if (!sChannel.finishConnect()) {
        key.cancel();
      }
    }

     if (key.isValid() && key.isWritable()) {
      logger.debug("Event found, isWritable");
      Object object = key.attachment();
      logger.debug("Object: " + object.toString());
      SocketChannel client = (SocketChannel) key.channel();
      logger.debug("Old Send Buffer Size: "
          + client.socket().getSendBufferSize());
      client.socket().setSendBufferSize(32768);
      logger.debug("New Send Buffer Size: "
          + client.socket().getSendBufferSize());
      ByteBuffer buffer = null;
      HttpMessageResponse response = null;
      if (object instanceof HttpMessageResponse) {
        response = (HttpMessageResponse) object;
        buffer = ByteBuffer.allocateDirect(1024 * 1024 * 10);
        buffer.clear();
        buffer.put(response.getStartLine().getBytes());
        buffer.put((byte) '\r');
        buffer.put((byte) '\n');
        buffer.put(response.getHeadersAsString().getBytes());
        buffer.put((byte) '\r');
        buffer.put((byte) '\n');
        if (response.getBodyContent() != null) {
          if (!response.isContentLengthSet()) {
            buffer.put(Integer.toHexString(response.getBodyContent().length)
                .getBytes());
            buffer.put((byte) '\r');
            buffer.put((byte) '\n');
            buffer.put(response.getBodyContent());
            buffer.put((byte) '\r');
            buffer.put((byte) '\n');
            buffer.put(new Integer(0).toString().getBytes());
            buffer.put((byte) '\r');
            buffer.put((byte) '\n');
            buffer.put((byte) '\r');
            buffer.put((byte) '\n');
          } else {
            buffer.put(response.getBodyContent());
          }
        }
        buffer.flip();
      } else if (object instanceof ByteBuffer) {
        buffer = (ByteBuffer) object;
      }
      /*
       * if( logger.isDebugEnabled() ) { byte[] buf = new byte[buffer.limit()];
       * buffer.get(buf); logger.debug("Ouput to Browser: \n"+ new
       * String(buf).replaceAll("[\r]?\n","\r\nWire==> ") ); buffer.flip(); }
       */
      try {
        int sendBufferSize = client.socket().getSendBufferSize();
        byte[] sendBuffer = new byte[sendBufferSize];
        while (buffer.position() < buffer.limit()) {
          int remaining = buffer.limit() - buffer.position();
          int writeSize =
              ((remaining > sendBufferSize) ? sendBufferSize : remaining);
          logger.debug("Remaining: " + remaining + " WriteSize: " + writeSize
              + " SendBufferSize: " + sendBufferSize);
          buffer.get(sendBuffer, 0, writeSize);
          client.write(ByteBuffer.wrap(sendBuffer, 0, writeSize));
          try {
            Thread.sleep(100);
          } catch (Exception ignored) {
          }
        }
        client.close();
      } catch (Exception e) {
        stopTransaction(startTimeStamp, ProxyStatistics.FAILURE);
        logger.error("Exception while returning the response: " + e, e);
        if (response != null)
          runHandlers(proxyRegistry.getHandlers(), response, e);
        client.close();
        return;
      }
    }
    stopTransaction(startTimeStamp, ProxyStatistics.SUCCESS);
  }
  
  /**
   * Run registered HttpMessageProcessors on a HttpMessage and return it or null
   * if doSend() returned false
   */
  private HttpMessageRequest runRequestProcessors(Vector procs,
      HttpMessageRequest message) {
    if (message == null) return null;
    boolean doSend = true;
    for (int i = 0; i < procs.size(); i++) {
      logger.debug("Processing Processor " + (i + 1) + " of " + procs.size());
      HttpMessageRequestProcessor hmreqp =
          (HttpMessageRequestProcessor) procs.elementAt(i);
      if (!hmreqp.doContinue(message)) break;
      message = hmreqp.process(message);
      if (message == null || !hmreqp.doSend(message)) {
        doSend = false;
        break;
      }
    }
    if (doSend == false) return null;
    return message;
  }

  private HttpMessageResponse runResponseProcessors(Vector procs,
      HttpMessageResponse message) {
    if (message == null) return null;
    boolean doSend = true;
    for (int i = 0; i < procs.size(); i++) {
      logger.debug("Processing Processor " + (i + 1) + " of " + procs.size());
      HttpMessageResponseProcessor hmp =
          (HttpMessageResponseProcessor) procs.elementAt(i);
      if (!hmp.doContinue(message)) break;
      message = hmp.process(message);
      if (message == null || !hmp.doSend(message)) {
        doSend = false;
        break;
      }
    }
    if (doSend == false) return null;
    return message;
  }

  
  /** method to determine if the target of this request is the proxy server itself */
  private boolean isLocalRequest(final String targetHost, final int targetPort) {
    logger.debug("Checking for local request: " + targetHost + ":" + targetPort
        + " == " + inetAddr.getHostName() + ":" + this.port + " or "
        + inetAddr.getHostAddress());
    if ((inetAddr.getHostName().startsWith(targetHost) || targetHost
        .equals(inetAddr.getHostAddress()))
        && targetPort == this.port) return true;
    return false;
  }

 

  private void processSecureConnection(SelectionKey key) throws IOException {
    if (key.isValid() && key.isWritable()
        && key.channel() instanceof SocketChannel) {
      SocketChannel client = (SocketChannel) key.channel();
      try {
        logger.debug("processSecureConnection Event found, isWritable");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        if (logger.isDebugEnabled()) {
          byte[] buf = new byte[buffer.limit()];
          buffer.get(buf);
          // logger.debug("Ouput to Browser(SSL): \n"+
          // new String(buf).replaceAll("[\r]?\n","\r\nWire==> ") );
          buffer.flip();
        }
        client.write(buffer);
        buffer.flip();
        /*
         * SSLEngine engine = initServer(); SSLByteChannel sslByteChannel = new
         * SSLByteChannel(client, engine);
         */

      } catch (Exception e) {
        logger.error("Exception while returning the response: " + e, e);
        client.close();
        return;
      }
    }
  }

  private HttpMessageRequest parseRequest(SocketChannel socket)
      throws Exception {
    HttpMessageRequest request = new HttpMessageRequest();
    Socket s = socket.socket();
    request.setFromHost(s.getLocalAddress().getHostAddress());
    request.setFromPort(s.getLocalPort());
    InputStream reader = s.getInputStream();
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
      logger.debug("request line: \"" + line + "\"");
      if (line.endsWith("\r\n\r\n")) break;
    }
    request.setHeaders(headers);
    logger.debug("Finished Reading Header of Request");
    char c;
    StringBuffer sb = new StringBuffer();
    while (is.ready()) {
      c = (char) is.read();
      sb.append(c);
    }
    if (sb.toString().getBytes().length > 0)
      request.addToBody(sb.toString().getBytes(),
          sb.toString().getBytes().length);
    logger.debug("Finished Reading Body of Request");
    return request;
  }

  /** Open a connection to the remote and execute it */
  private HttpMessageResponse executeRequest(HttpMessageRequest request)
      throws Exception {
    URL url = request.getUri().toURL();
    logger.debug("Opening socket to: " + url);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setFollowRedirects(false);
    connection.setDoOutput(true);
    // connection.setDoInput(true);
    Iterator it = request.getHeaders().keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      if (!key.equals(HttpMessage.HEADER_ACCEPT_ENCODING)) {
        Vector items = (Vector) request.getHeaders().get(key);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < items.size(); i++) {
          sb.append((String) items.elementAt(i));
          if ((i + 1) < items.size()) sb.append(", ");
        }
        connection.setRequestProperty(key, sb.toString());
        logger.debug("Setting Header: " + key + ": " + sb.toString());
      } else {
        // TODO add handle for compressed response
        logger.debug("Ignoring Accept-Encoding Header");
      }
    }

    if (request.getBodyContent() != null) {
      /*
       * OutputStreamWriter os = new OutputStreamWriter( new
       * BufferedOutputStream(connection.getOutputStream() ), "8859_1");
       * os.write( new String(request.getBodyContent()) );
       * logger.debug("sending: "+ request.getBodyContent()); os.flush();
       * os.close();
       */
      OutputStream writer = connection.getOutputStream();
      writer.write(request.getBodyContent());
      logger.debug("Size of write: " + request.getBodyContent().length);
    }

    HttpMessageResponse response = new HttpMessageResponse();
    response.setStartLine(connection.getHeaderField(0));
    logger.debug("Response: " + response.getStartLine());
    response.setStatusCode(connection.getResponseCode());
    response.setReasonPhrase(connection.getResponseMessage());
    // response.setHeaders( connection.getHeaderFields() );
    boolean done = false;
    for (int num = 1; !done; num++) {
      String key = connection.getHeaderFieldKey(num);
      String value = connection.getHeaderField(num);
      if (key != null && value != null) {
        response.addHeader(key, value);
      } else {
        done = true;
      }
    }
    int total = 0;
    if (response.getStatusCode() != 404) {
      InputStream reader = null;
      try {
        reader = connection.getInputStream();
        while (reader != null) {
          byte[] buff = new byte[1024 * 50];
          int size = reader.read(buff, 0, buff.length);
          if (size > -1) total += size;
          logger.debug("Size of read: " + size + " subtotal: " + total);
          if (size < 1) break;
          response.addToBody(buff, size);
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        if (reader != null) reader.close();
      }
    }
    if (logger.isDebugEnabled() && response.getBodyContent() != null) {
      BufferedReader br =
          new BufferedReader(new InputStreamReader(response
              .getBodyContentStream()));
      char c;
      StringBuffer sb = new StringBuffer();
      while (br.ready()) {
        c = (char) br.read();
        sb.append(c);
      }
      logger.debug("Set Body To: \n" + sb.toString());
      br.close();
    }
    return response;
  }

  /** Initialized SSL Server if ssl is configured */
  /*
   * private SSLEngine initServer() { if( keyfile == null ) { logger.info("SSL
   * Not enabled, running unencrypted proxy only"); return null; } try {
   * logger.debug("Starting SSL Init"); KeyStore ks =
   * KeyStore.getInstance("JKS"); ks.load( new FileInputStream(keyfile),
   * keystorePass);
   * 
   * KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
   * kmf.init(ks,keystoreKeysPass);
   * 
   * TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
   * tmf.init(ks);
   * 
   * SSLContext context = SSLContext.getInstance("TLS");
   * context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
   * 
   * SSLEngine engine = context.createSSLEngine();
   * engine.setUseClientMode(false); engine.beginHandshake();
   * logger.debug("Finished SSL Init"); return engine; } catch( Exception e ) {
   * logger.fatal("Error creating SSLEngine Exception: "+ e,e); } return null; }
   */
  private static class WorkAroundX509TrustManager implements X509TrustManager {
    /*
     * public boolean isClientTrusted(X2509Certificate[] chain){ return true; }
     * public boolean isServerTrusted(X509Certificate[] chain){ return true; }
     * public X509Certificate[] getAcceptedIssuers(){ return null; }
     */
    private X509Certificate[] c;

    public void checkClientTrusted(X509Certificate[] chain, String type) {
      c = chain;
    }

    public void checkServerTrusted(X509Certificate[] chain, String type) {
      c = chain;
    }

    public X509Certificate[] getAcceptedIssuers() {
      return c;
    }

  }


    private class SSLServerThread extends Thread {
    private int securePort = -1;
    private String targetHost = "";
    private int targetPort = 443;
    private Socket clientSocket = null;
    private SSLServerSocket serverSocket = null;
    private SSLSocket targetSocket = null;
    private SSLSocket localClientSocket = null;
    private boolean run = false;

    public boolean isRunning() {
      return run;
    }

    public void setRunning(boolean b) {
      run = b;
    }

    public SSLServerThread(int localSecurePort, String host, int port,
        Socket socket) {
      super();
      securePort = localSecurePort;
      targetHost = host;
      targetPort = port;
      clientSocket = socket;

      KeyStore ks;
      KeyManagerFactory kmf;
      KeyManager[] km;
      X509TrustManager tm = new WorkAroundX509TrustManager();
      TrustManager[] tma = {tm};
      SSLContext context;
      SSLServerSocketFactory sockFactory = null;


      logger.debug("Starting SSL Init");
      try {
        // This code creates a local ssl server for our man-in-the-middle stuff
        ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(proxyRegistry.getKeystoreFilename()),
            proxyRegistry.getKeystorePassword());
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, proxyRegistry.getKeystoreKeysPassword());
        km = null;

        /*
         * TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
         * tmf.init(ks); context.init(kmf.getKeyManagers(),
         * tmf.getTrustManagers(), null);
         */

        context = SSLContext.getInstance("SSL");
        context.init(km, tma, null);
        sockFactory = context.getServerSocketFactory();
      } catch (Exception e) {
        logger.error("Error Initializing SSL: " + e, e);
      }

      boolean connected = false;
      while (!connected) {
        try {
          logger.info("Trying to open local ssl server on port: "
              + localSecurePort);
          serverSocket =
              (SSLServerSocket) sockFactory.createServerSocket(localSecurePort);
          connected = true;
        } catch (Exception e) {
          connected = false;
          localSecurePort++;
        }
      }

      try {
        String[] supported = serverSocket.getSupportedCipherSuites();
        String[] anonSupported = new String[supported.length];
        int numAnonSupported = 0;
        for (int i = 0; i < supported.length; i++) {
          if (supported[i].indexOf("_anon_") > 0)
            anonSupported[numAnonSupported++] = supported[i];
        }

        String[] oldEnabled = serverSocket.getEnabledCipherSuites();
        String[] newEnabled = new String[oldEnabled.length + numAnonSupported];
        System.arraycopy(oldEnabled, 0, newEnabled, 0, oldEnabled.length);
        System.arraycopy(anonSupported, 0, newEnabled, oldEnabled.length,
            numAnonSupported);

        serverSocket.setEnabledCipherSuites(newEnabled);

        // Create the connection to the actual intended target:
        SSLSocketFactory clientSocketFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
        targetSocket =
            (SSLSocket) clientSocketFactory
                .createSocket(targetHost, targetPort);
        targetSocket.setEnabledCipherSuites(targetSocket
            .getSupportedCipherSuites());
        targetSocket.setUseClientMode(true);

        logger.debug("Finished SSL Init");
      } catch (Exception e) {
        logger.error("Error Initializing SSL: " + e, e);
      }

      /*
       * SSLEngine engine = context.createSSLEngine();
       * engine.setUseClientMode(false); engine.beginHandshake();
       */
    }

    public void run() {
      logger.info("Listening for connection");
      try {
        LocalServer localServerThread = new LocalServer(serverSocket);
        localServerThread.start();
        logger.debug("Started local server");
        SSLSocketFactory clientSocketFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
        localClientSocket =
            (SSLSocket) clientSocketFactory.createSocket("localhost",
                securePort);
        localClientSocket.setEnabledCipherSuites(serverSocket
            .getSupportedCipherSuites());
        localClientSocket.setUseClientMode(true);
        logger.debug("Started local client");
        while (localServerThread.socket == null) {
        }
        Socket server = localServerThread.socket;
        logger.debug("Connected local client to local server");
        IORedirector c2l, l2c, l2s, s2l, s2t, t2s;

        // write the connect dialog to the client
        OutputStream os = clientSocket.getOutputStream();
        byte[] msg =
            new String(
                "HTTP/1.0 200 Connection established\r\nProxy-agent: WPG-RecordingProxy/1.0\r\n\r\n")
                .getBytes();
        os.write(msg);

        IORedirector c2s, s2c;
        c2s =
            new IORedirector(clientSocket.getInputStream(), targetSocket
                .getOutputStream());
        c2s.setName("remoteClient->remoteServer");
        c2s.start();
        Thread.sleep(500);

        s2c =
            new IORedirector(targetSocket.getInputStream(), clientSocket
                .getOutputStream());
        s2c.setName("remoteServer->remoteClient");
        s2c.start();
        Thread.sleep(500);

        /*
         * let's just get a regular proxy working for now, so the below is
         * commented out /** send all client requests on to the local client * /
         * c2l = new IORedirector( clientSocket.getInputStream(),
         * localClientSocket.getOutputStream());
         * c2l.setName("remoteCleint->localClient"); c2l.start();
         * Thread.sleep(500);
         * 
         * /** send all local client requests on to the client * / l2c = new
         * IORedirector( localClientSocket.getInputStream(),
         * clientSocket.getOutputStream());
         * l2c.setName("localClient->remoteClient"); l2c.start();
         * Thread.sleep(500);
         * 
         * /** send all client requests on to the local ssl server * / l2s = new
         * IORedirector( localClientSocket.getInputStream(),
         * server.getOutputStream()); l2s.setName("localClient->localServer");
         * l2s.start(); Thread.sleep(500);
         * 
         * /**forward all local ssl server input to the client output * / s2l =
         * new IORedirector( server.getInputStream(),
         * localClientSocket.getOutputStream());
         * s2l.setName("localServer->localClient"); s2l.start();
         * Thread.sleep(500);
         * 
         * /**forward all server traffic to target server * / s2t = new
         * IORedirector( server.getInputStream(),
         * targetSocket.getOutputStream());
         * s2t.setName("localServer->remoteServer"); s2t.start();
         * Thread.sleep(500);
         * 
         * t2s = new IORedirector( targetSocket.getInputStream(),
         * server.getOutputStream() ); t2s.setName("remoteServer->localServer");
         * t2s.start(); Thread.sleep(500);
         * 
         * logger.debug("Everything setup, start another handshake");
         * localClientSocket.startHandshake();
         */
      } catch (IOException e) {
        logger
            .error("IOException while starting IORedirector Threads: " + e, e);
        return;
      } catch (InterruptedException e) {
      }

      /*
       * run=true; while(run) { }
       */
    }

    /** internal thread to read packets from client and forward to remote */
    private class IORedirector extends Thread {
      protected InputStream in = null;
      protected OutputStream out = null;
      private byte[] buffer = new byte[4096];
      public boolean run = false;

      public IORedirector(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
      }

      public void run() {
        run = true;
        logger.debug("Starting: " + getName());
        try {
          while (run) {
            int length = in.read(buffer);
            if (length == 0) break;
            out.write(buffer, 0, length);
          }
          run = false;
        } catch (IOException e) {
          logger.error("IOException: " + e, e);
          run = false;
        } finally {
          try {
            in.close();
          } catch (Exception ignored) {
          }
          try {
            out.close();
          } catch (Exception ignored) {
          }
        }
      }
    }

    /** Server thread to assist in connecting the client to the local server */
    private class LocalServer extends Thread {
      public SSLServerSocket serverSocket;
      public Socket socket;

      public LocalServer(SSLServerSocket s) {
        serverSocket = s;
      }

      public void run() {
        try {
          socket = serverSocket.accept();
        } catch (Exception e) {
          logger.error("Socket Accept Exception: " + e, e);
          return;
        }
        while (socket != null) {
        }
      }
    }

  }

  /*
   * internal method to collect the statistics and return that information to
   * the browser/user making the request
   */
  private void processLocalRequest(HttpMessageRequest request,
      SocketChannel client) {
    logger.debug("Processing a local statistics request");
    try {
      client.configureBlocking(false);
      SelectionKey clientKey = client.register(selector, SelectionKey.OP_WRITE);
      ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 10);
      buf.clear();
      buf.put("HTTP/1.0 200 OK".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put("Server: WPG-Proxy/1.0".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf
          .put("cache-control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0"
              .getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put("pragma: no-cache".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put("connection: close".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put("transfer-encoding: chunked".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put("content-type: text/html".getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.put(ProxyStatistics.getSingleton().getHTMLPage().getBytes());
      buf.put((byte) '\r');
      buf.put((byte) '\n');
      buf.flip();
      clientKey.attach(buf);
    } catch (Exception e) {
      logger.error("Exception while returning statistics web page: " + e, e);
    }
    return;
  }

  /*
   * public void spoofSSH( Socket s, RequestObject ro ) throws Exception {
   * //browser streams InputStreamReader userIS = new InputStreamReader(
   * s.getInputStream() ); OutputStreamWriter userOS = new
   * OutputStreamWriter(s.getOutputStream());
   * 
   * //temp internal server sockets and streams logger.debug("trying to setup
   * local ssl server"); ServerSocket ts = getSecureServerSocket(securePort);
   * LocalSSLServer lssT = new LocalSSLServer(); lssT.serverSock = ts; URL url =
   * new URL("https://"+ ro.url); lssT.clientSock =
   * getSecureClientSocket(url.getHost(), url.getPort() ); lssT.start();
   * logger.debug("local ssl server setup"); logger.debug("trying to connect
   * local ssl client to local ssl server"); SSLSocket tcs =
   * getSecureClientSocket("localhost",securePort); logger.debug("local ssl
   * client connected to local ssl server"); InputStreamReader tcin = new
   * InputStreamReader( tcs.getInputStream() ); OutputStreamWriter tcout = new
   * OutputStreamWriter(tcs.getOutputStream());
   * 
   * //destination host socket and stream
   * 
   * //now that everything is set, tell the browser to continue
   * userOS.write("HTTP/1.1 200 Connection Established\r\n");
   * logger.info("HTTP/1.1 200 Connection Established");
   * userOS.write("Proxy-agent: WPG-RecordingProxy/1.0\r\n" );
   * logger.info("Proxy-agent: WPG-RecordingProxy/1.0" ); userOS.write("\r\n");
   * userOS.flush(); int ch; StringBuffer sb = new StringBuffer();
   * logger.debug("entering read from browser -> write to internal ssl"); while(
   * (ch = userIS.read() ) != -1 ) { tcout.write(ch); sb.append(ch); }
   * sb.append("\nBREAK\n"); logger.debug("entering read from internal ssl ->
   * write to browser"); while( (ch = tcin.read() ) != -1 ) { userOS.write(ch);
   * sb.append(ch); } logger.debug("Request: "+ sb.toString()); }
   * 
   */
  /** Creates a new instance of ProxyProcessor */
  public ProxyProcessor(boolean secure, InetAddress inetAddr, SelectionKey key,
      ProxyRegistry proxyRegistry) throws IOException {
    this.proxyRegistry = proxyRegistry;
    this.inetAddr = inetAddr;
    if (!secure) {
      selector = key.selector();
      processConnection(key);
    } else {
      secureSelector = key.selector();
      processSecureConnection(key);
    }
  }
}
