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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
   * Executes an HTTP GET and return the response.
   * @param request The request.
   * @return The HTTP response.
   * @throws StrategyException
   */
  HttpResponse getHttpResponse(FetchRequest request) throws StrategyException {
    try {
      // Simply do a HTTP GET
      LOG.info(request.getId() + ": Requesting resource: " + request.getResource());
      HttpGet httpGet = new HttpGet(request.getResource());
      return httpClient.execute(httpGet);
    } catch (IOException e) {
      throw new StrategyException(request.getId() + ": while executing HTTP GET:", e);
    }
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
