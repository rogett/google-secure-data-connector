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
package com.google.dataconnector.client;

import com.google.common.base.Preconditions;
import com.google.dataconnector.client.socketsession.SocketSessionManager;
import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionData;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionReply;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionReply.Status;
import com.google.dataconnector.util.ClockUtil;
import com.google.dataconnector.util.SdcKeysManager;
import com.google.dataconnector.util.SessionEncryption;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


/**
 * Handler for the SocketSessionRequest message.
 * 
 * @author dchung@google.com (David Chung)
 *
 */
public class SocketSessionRequestHandler implements Dispatchable {

  private static Logger LOG = Logger.getLogger(SocketSessionRequestHandler.class);

  // Injected Dependencies.
  private final SdcKeysManager sdcKeysManager;
  private final SocketSessionManager sessionManager;
  private final Injector injector;
  private final ClockUtil clock;

  // Runtime Dependencies.
  private FrameSender frameSender;
  private Sink<SocketSessionData> tunnel;
  
  /**
   * A data sink of type T.  It's some interface that is able to receive the
   * data from the socket (its InputStream).
   * @param <T> The transformed data type created from data from InputStream.
   */
  public interface Sink<T> {
    public boolean receive(T data);
  }
  
  @Inject
  public SocketSessionRequestHandler(SdcKeysManager km, SocketSessionManager manager,
      Injector injector, ClockUtil clock) {
    this.sdcKeysManager = km;
    this.sessionManager = manager;
    this.injector = injector;
    this.clock = clock;
  }

  public final void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
    this.tunnel = new Sink<SocketSessionData>() {
      @Override
      public boolean receive(SocketSessionData message) {
        return sendToCloud(message);
      }
    };
  }


  @Override
  public void dispatch(FrameInfo frameInfo) throws FramingException {
    // Session encryption: decrypt the message from the cloud:
    if (!this.sdcKeysManager.hasSessionEncryption()) {
      LOG.warn("Cannot decrypt message for fetch protocol: no session encryption.");
      return;
    }


    try {

      SocketSessionData dataFromTunnel = 
        sdcKeysManager.getSessionEncryption().getFrom(frameInfo,
          new SessionEncryption.Parse<SocketSessionData>() {
          public SocketSessionData parse(ByteString s) throws InvalidProtocolBufferException {
            return SocketSessionData.parseFrom(s);
          }
      });
      
      if (dataFromTunnel == null) {
        LOG.warn("Cannot decrypt data from tunnel.  Dropping request.");
        return;
      }
      
      // This is the case where data is coming from the cloud.  This should
      // happen more frequently than the requests for connection / close.
      handleSocketSessionData(dataFromTunnel);

    } catch (InvalidProtocolBufferException e) {
      // See if this is request.
      SocketSessionRequest request = null;
      SocketSessionReply.Builder replyBuilder = null;
      try {
        request = 
          sdcKeysManager.getSessionEncryption().getFrom(frameInfo,
              new SessionEncryption.Parse<SocketSessionRequest>() {
              public SocketSessionRequest parse(ByteString s) 
                throws InvalidProtocolBufferException {
                return SocketSessionRequest.parseFrom(s);
              }
          });

        replyBuilder = SocketSessionReply.newBuilder()
          .setSocketHandle(request.getSocketHandle())
          .setVerb(request.getVerb())
          .setHostname(request.getHostname())
          .setPort(request.getPort());

        long start = this.clock.currentTimeMillis();
        handleSocketSessionRequest(request, replyBuilder);
        replyBuilder.setLatency(this.clock.currentTimeMillis() - start);
        SocketSessionReply reply = replyBuilder.build();
        sendToCloud(reply);
        this.sessionManager.notifySent(reply.getSocketHandle(), reply);
      } catch (InvalidProtocolBufferException e2) {
        LOG.warn("Unknown message type: " + frameInfo.getType() +
            ":" + frameInfo);
          throw new FramingException("Unknown message type: " + frameInfo.getType() +
              ":" + frameInfo);
      }
    }
  }
  
  protected void handleSocketSessionRequest(SocketSessionRequest request,
      SocketSessionReply.Builder replyBuilder) {
    LOG.debug(String.format("SocketSessionRequest handle=%s,verb=%s", 
        request.getSocketHandle().toStringUtf8(), request.getVerb()));
    switch (request.getVerb()) {
      case CREATE:
        // First resolve.
        InetSocketAddress endpoint = resolve(request.getSocketHandle(),
            request.getHostname(), request.getPort());
        if (endpoint == null) {
          replyBuilder.setStatus(Status.UNKNOWN_HOST);
        } else {
          // Update with the resolved address:
          replyBuilder.setHostname(endpoint.getAddress().getCanonicalHostName());
          // Now create the session:
          boolean success = this.sessionManager.createSession(
              this.tunnel,
              request.getSocketHandle(), endpoint);
          if (success) {
            replyBuilder.setStatus(Status.OK);
          } else {
            replyBuilder.setStatus(Status.ERROR);
          }
        }
        break;
      case CONNECT:
        if (this.sessionManager.connect(request.getSocketHandle())) {
          replyBuilder.setStatus(Status.OK);
        } else {
          replyBuilder.setStatus(Status.CANNOT_CONNECT);
        }
        break;
      case CLOSE:
        if (this.sessionManager.close(request.getSocketHandle())) {
          replyBuilder.setStatus(Status.OK);
        } else {
          replyBuilder.setStatus(Status.ERROR);
        }
        break;
        default:
          LOG.warn("Unknown message type: " + request.getVerb() +
              ":" + request.toString());
    }
  }
  
  protected void handleSocketSessionData(SocketSessionData data) {
    LOG.debug("WRITE " + data.getData().size() + " bytes, data = [" +
        new String(data.getData().toByteArray()) + "]");
    this.sessionManager.write(data.getSocketHandle(), data.getData().toByteArray(),
        data.getStreamOffset());
  }
  
  private InetSocketAddress resolve(ByteString handle, String hostname, int port) {
    try {
      InetAddress found = InetAddress.getByName(hostname);
      return new InetSocketAddress(found, port);
    } catch (UnknownHostException e) {
      LOG.warn(handle.toStringUtf8() + ": Host unknown: " + hostname, e);
    }
    return null;
  }
  
  /**
   * Asynchronously sends the reply to the cloud.
   * @param reply The reply.
   */
  boolean sendToCloud(SocketSessionReply reply) {
    Preconditions.checkNotNull(frameSender);
    // Encrypt the reply.
    // Session encryption: decrypt the message from the cloud:
    if (!this.sdcKeysManager.hasSessionEncryption()) {
      LOG.warn("Cannot encrypt message for fetch protocol: no session encryption. Not sent.");
      return false;
    }

    LOG.debug("REPLY: handle=" + reply.getSocketHandle().toStringUtf8() +
        ": verb=" + reply.getVerb() + ", status=" + reply.getStatus() +
        ", latency=" + reply.getLatency());
    LOG.debug("Sending reply =" + reply);
    
    FrameInfo frame = this.sdcKeysManager.getSessionEncryption().toFrameInfo(
        FrameInfo.Type.SOCKET_SESSION, reply);

    frameSender.sendFrame(FrameInfo.Type.SOCKET_SESSION, reply.toByteString());
    return true;
  }
  
  /**
   * Asynchronously sends data to the cloud.
   * @param data The data.
   */
  boolean sendToCloud(SocketSessionData data) {
    Preconditions.checkNotNull(frameSender);
    LOG.debug("DATA: handle=" + data.getSocketHandle().toStringUtf8() +
        ", offset=" + data.getStreamOffset() + ", data=" + data.getData().toStringUtf8());
    frameSender.sendFrame(FrameInfo.Type.SOCKET_SESSION, data.toByteString());
    return true;
  }
}
