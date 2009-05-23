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
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo.Type;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tests for the {@link InputStreamConnector} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class InputStreamConnectorTest extends TestCase {

  private static final int CONNECTION_ID = 0;
  
  private FrameSender frameSender;
  private ByteArrayInputStream bis;
  private BlockingQueue<FrameInfo> sendQueue;
  private byte[] expectedPayload = new byte[] { 1, 2, 3, 4, 5, 6 };
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    bis = new ByteArrayInputStream(expectedPayload); 
    sendQueue = new LinkedBlockingQueue<FrameInfo>(1000);
    frameSender = new FrameSender(sendQueue);
  }
  
  public void testReceiveInputAndCreateFrames() throws Exception {
    
    SocketDataInfo expectedContinueSdi = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setSegment(ByteString.copyFrom(expectedPayload))
        .setState(SocketDataInfo.State.CONTINUE)
        .build();
    FrameInfo expectedContinueFrame = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.SOCKET_DATA)
        .setPayload(expectedContinueSdi.toByteString())
        .build();
    
    SocketDataInfo expectedClosingSdi = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.CLOSE)
        .build();
    FrameInfo expectedClosingFrame = FrameInfo.newBuilder()
        .setType(Type.SOCKET_DATA)
        .setPayload(expectedClosingSdi.toByteString())
        .build();
    
    MockConnectionRemover connectionRemover = new MockConnectionRemover();
    
    InputStreamConnector inputStreamConnector = new InputStreamConnector();
    inputStreamConnector.setInputStream(bis);
    inputStreamConnector.setConnectionId(CONNECTION_ID);
    inputStreamConnector.setFrameSender(frameSender);
    inputStreamConnector.setConnectorStateCallback(connectionRemover);
    inputStreamConnector.start(); // LARGE TEST.  
    
    FrameInfo actualContinueFrame = sendQueue.take();
    assertEquals(expectedContinueFrame, actualContinueFrame);
    FrameInfo actualClosingFrame = sendQueue.take();
    assertEquals(expectedClosingFrame, actualClosingFrame);
  }
  
  public void testIOExceptionInputStream() throws Exception {
    
    SocketDataInfo expectedClosingSdi = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.CLOSE)
        .build();
    FrameInfo expectedClosingFrame = FrameInfo.newBuilder()
        .setType(Type.SOCKET_DATA)
        .setPayload(expectedClosingSdi.toByteString())
        .build();
    
    InputStream mockIs = EasyMock.createMock(InputStream.class);
    EasyMock.expect(mockIs.read(EasyMock.isA(byte[].class)))
        .andThrow(new IOException("read error"));
    EasyMock.replay(mockIs);
    
    MockConnectionRemover connectionRemover = new MockConnectionRemover();
    
    InputStreamConnector inputStreamConnector = new InputStreamConnector();
    inputStreamConnector.setInputStream(mockIs);
    inputStreamConnector.setConnectionId(CONNECTION_ID);
    inputStreamConnector.setFrameSender(frameSender);
    inputStreamConnector.setConnectorStateCallback(connectionRemover);
    inputStreamConnector.start(); // LARGE TEST.  
    
    FrameInfo actualClosingFrame = sendQueue.take();
    assertEquals(expectedClosingFrame, actualClosingFrame);
    EasyMock.verify(mockIs);
  }
  
  /**
   * Verifies the callback is called correctly.
   * 
   * TODO(rayc) does not currently detect its been called due to multi threaded nature of test.
   * If we were to wait or sleep until this completes, we would create a flakey test.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public static class MockConnectionRemover implements ConnectorStateCallback {
    
    private boolean callbackFired = false;
    
    @Override
    public void close(int connectionId) {
      if (connectionId != CONNECTION_ID) {
        throw new RuntimeException("Connection IDs mismatch: expected " + CONNECTION_ID + 
            " actual " + connectionId);
      } else {
        callbackFired = true;
      }
    }
    
    public boolean isCallbackFired() {
      return callbackFired;
    }
  }
}
