/* Copyright 2009 Google Inc.
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
package com.google.dataconnector.protocol;

import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.dataconnector.util.Preconditions;
import com.google.protobuf.ByteString;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads bytes off the supplied {@link InputStream}, creates the SocketDataInfo and sends them
 * using the supplied {@link FrameSender}.  If it detects and inputstream close, it will fire
 * off a CLOSE SocketDataInfo informing the otherside. 
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class InputStreamConnector extends Thread {
  
  private static final Logger LOG = Logger.getLogger(InputStreamConnector.class);
  
  private InputStream inputStream;
  private int connectionId;
  private FrameSender frameSender;
  private ConnectorStateCallback connectorStateCallback;
   
  /**
   * Reads bytes from the input stream and whatever is returned packages into a 
   * {@link SocketDataInfo} and sends using the supplied FrameReceiver.  If it detects
   * a close on the input stream it will fire the {@link ConnectorStateCallback}.
   */
  @Override
  public void run() {
    Preconditions.checkNotNull(inputStream, "must set inputStream before calling start()");
    Preconditions.checkNotNull(connectionId, "must set connectionId before calling start()");
    
      try {
        byte[] buffer = new byte[65536];
        while (true) {
          int bytesRead;
          bytesRead = inputStream.read(buffer);
          if (bytesRead == -1) {
            LOG.debug("Input stream " + connectionId + " closed.");
            // send closing frame
            frameSender.sendFrame(FrameInfo.Type.SOCKET_DATA, SocketDataInfo.newBuilder()
                .setConnectionId(connectionId)
                .setState(SocketDataInfo.State.CLOSE)
                .build().toByteString());
            LOG.trace("Sent closing frame for connection: " + connectionId);
            break;
          }
          frameSender.sendFrame(FrameInfo.Type.SOCKET_DATA, SocketDataInfo.newBuilder()
              .setConnectionId(connectionId)
              .setSegment(ByteString.copyFrom(buffer, 0, bytesRead))
              .setState(SocketDataInfo.State.CONTINUE)
              .build().toByteString());
        }
      } catch (IOException e) {
        // This is probably caused by a socket shutdown or error on ourside, let the
        // other side know the connection is done.
        frameSender.sendFrame(FrameInfo.Type.SOCKET_DATA, SocketDataInfo.newBuilder()
            .setConnectionId(connectionId)
            .setState(SocketDataInfo.State.CLOSE)
            .build().toByteString());
      } 
    connectorStateCallback.close(connectionId);
    LOG.debug("removed connectionId " + connectionId);
  }
  
  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public void setConnectionId(int connectionId) {
    this.connectionId = connectionId;
  }
  
  public void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  public void setConnectorStateCallback(ConnectorStateCallback connectorStateCallback) {
    this.connectorStateCallback = connectorStateCallback;
  }
}
