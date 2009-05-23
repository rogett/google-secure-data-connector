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

import com.google.dataconnector.protocol.InputStreamConnectorTest.MockConnectionRemover;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tests for the {@link OutputStreamConnector} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class OutputStreamConnectorTest extends TestCase {
  private static final int CONNECTION_ID = 0;

  private BlockingQueue<SocketDataInfo> sendQueue;
  private ByteArrayOutputStream bos;
  private byte[] expectedPayload = new byte[] { 1, 2, 3, 4, 5, 6 };  
  
  private SocketDataInfo expectedClosingSdi; 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    expectedClosingSdi = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.CLOSE)
        .build();
    
    sendQueue = new LinkedBlockingQueue<SocketDataInfo>(); 
    bos = new ByteArrayOutputStream();
     
  }
  
  // LARGE
  public void testOutputReceived() throws Exception {
    
    MockConnectionRemover mcr = new MockConnectionRemover();
    OutputStreamConnector outputStreamConnector = new OutputStreamConnector(sendQueue);
    outputStreamConnector.setConnectionId(CONNECTION_ID);
    outputStreamConnector.setOutputStream(bos);
    outputStreamConnector.setConnectorStateCallback(mcr);
    outputStreamConnector.start(); // LARGE TEST 
    
    // Warning flakey test ahead.
    sendQueue.put(expectedClosingSdi);
    // We sleep this much to ensure that the outputstreamconnector has taken
    // the frame off the queue and written it to the bos.
    Thread.sleep(50); 
    Arrays.equals(expectedPayload, bos.toByteArray());
  }
  
  public void testOutputClosed() throws Exception {
    OutputStream mockOutputStream = EasyMock.createMock(OutputStream.class);
    mockOutputStream.close();
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputStream);
    
    MockConnectionRemover mcr = new MockConnectionRemover();
    OutputStreamConnector outputStreamConnector = new OutputStreamConnector(sendQueue);
    outputStreamConnector.setConnectionId(CONNECTION_ID);
    outputStreamConnector.setOutputStream(mockOutputStream);
    outputStreamConnector.setConnectorStateCallback(mcr);
    outputStreamConnector.start(); // LARGE TEST 
    
    // Warning flakey test ahead.
    sendQueue.put(expectedClosingSdi);
    // We sleep this much to ensure that the outputstreamconnector has taken
    // the frame off the queue and written it to the bos.
    Thread.sleep(50); 
    assertTrue(mcr.isCallbackFired());
    EasyMock.verify(mockOutputStream);
  }
}

