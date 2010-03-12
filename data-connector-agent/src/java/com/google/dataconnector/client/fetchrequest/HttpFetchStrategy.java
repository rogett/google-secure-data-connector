/* Copyright 2010 Google Inc.
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
package com.google.dataconnector.client.fetchrequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

import com.google.dataconnector.client.StrategyException;
import com.google.dataconnector.client.FetchRequestHandler.Strategy;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.MessageHeader;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;

/**
TODO(dchung): javadoc.
 */
public class HttpFetchStrategy implements Strategy {

  private static Logger LOG = Logger.getLogger(HttpFetchStrategy.class);

  // Local fields.
  private final DefaultHttpClient httpClient = new DefaultHttpClient();

  @Inject
  public HttpFetchStrategy() {
    // Default constructor.
  }

  /**
   * Executes an HTTP GET/POST and return the response.
   * @param request The request.
   * @return The HTTP response.
   * @throws StrategyException
   */
  HttpResponse getHttpResponse(FetchRequest request) throws StrategyException {
    HttpRequestBase httpMethod = null;
    try {
      httpMethod = getMethod(request);
      if (httpMethod != null) {
        copyHeaders(request, httpMethod);
        return httpClient.execute(httpMethod);
      } else {
        throw new StrategyException(request.getId() + ": Unknown method.");
      }
    } catch (IOException e) {
      String method = (httpMethod != null) ? httpMethod.getMethod() : "Unknown";
      throw new StrategyException(request.getId() + ": while executing HTTP " +
                                  method + ": ", e);
    }
  }

  /**
   * Based on the inbound request type header, determine the correct http
   * method to use.  If a method cannot be determined (or not specified),
   * HTTP GET is attempted.
   */
  HttpRequestBase getMethod(FetchRequest request) {
    String method = null;
    for (MessageHeader h : request.getHeadersList()) {
      if ("x-sdc-http-method".equalsIgnoreCase(h.getKey())) {
        method = h.getValue().toUpperCase();
      }
    }

    LOG.info(request.getId() + ": method=" + method + ", resource=" + request.getResource() +
             ((request.hasContents()) ? ", payload_size=" + request.getContents().size() : ""));

    if (method == null) {
      LOG.info(request.getId() + ": No http method specified. Default to GET.");
      method = "GET";
    }

    if ("GET".equals(method)) {
      return new HttpGet(request.getResource());
    }
    if ("POST".equals(method)) {
      HttpPost httpPost = new HttpPost(request.getResource());
      if (request.hasContents()) {
        LOG.debug(request.getId() + ": Content = " + new String(request.getContents().toByteArray()));
        httpPost.setEntity(new ByteArrayEntity(request.getContents().toByteArray()));
      }
      return httpPost;
    }
    if ("PUT".equals(method)) {
      HttpPut httpPut = new HttpPut(request.getResource());
      if (request.hasContents()) {
        LOG.debug(request.getId() + ": Content = " + new String(request.getContents().toByteArray()));
        httpPut.setEntity(new ByteArrayEntity(request.getContents().toByteArray()));
      }
      return httpPut;
    }
    if ("DELETE".equals(method)) {
      return new HttpDelete(request.getResource());
    }
    if ("HEAD".equals(method)) {
      return new HttpHead(request.getResource());
    }
    LOG.info(request.getId() + ": Unknown method " + method);
    return null;
  }

  /**
   * Copies the headers from the inbound request proto to the actual http request.
   */
  void copyHeaders(FetchRequest request, HttpRequestBase httpRequest) throws IOException {
      for (MessageHeader h : request.getHeadersList()) {
        if ("x-sdc-agent-cookie".equalsIgnoreCase(h.getKey())) {
          // set the cookie
          continue;
        }
        Header httpHeader = new BasicHeader(h.getKey(), h.getValue());
        LOG.debug(request.getId() + ":  Header = " + h.getKey() + ", " + h.getValue());
        httpRequest.addHeader(httpHeader);
      }
      // Tell the server to close down for keep-alive connections.
      httpRequest.addHeader(new BasicHeader("Connection", "close"));
  }

  /**
   * Implements the strategy method of processing the request and filling in the
   * reply with results of processing.
   * 
   * @param request The request.
   * @param replyBuilder The reply to fill in.
   */
  @Override
  public void process(FetchRequest request, FetchReply.Builder replyBuilder) 
  throws StrategyException {
    HttpResponse response = getHttpResponse(request);

    StatusLine statusLine = response.getStatusLine();
    int statusCode = statusLine.getStatusCode();
    replyBuilder.setStatus(statusCode);

    HttpEntity entity = response.getEntity();
    if (entity != null) {
      try {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        entity.writeTo(buff);
        buff.flush();
        buff.close();
        if (buff.size() > 0) {
          replyBuilder.setContents(ByteString.copyFrom(buff.toByteArray()));
        }
      } catch (IOException e) {
        throw new StrategyException(request.getId() + " while copying content:", e);
      }
    }
    // Copy the headers
    for (Header h : response.getAllHeaders()) {
      replyBuilder.addHeaders(MessageHeader.newBuilder()
          .setKey(h.getName()).setValue(h.getValue()).build());
    }
    LOG.info(request.getId() + ": Got response from resource:" + statusLine);
  }
}
