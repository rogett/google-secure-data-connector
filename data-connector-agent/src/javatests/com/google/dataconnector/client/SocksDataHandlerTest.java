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
package com.google.dataconnector.client;

import com.google.dataconnector.client.SocksDataHandler.ConnectionRemover;
import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.InputStreamConnector;
import com.google.dataconnector.protocol.OutputStreamConnector;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.SocketFactory;

public class SocksDataHandlerTest extends TestCase {

  private static final String DATA= "byte data to be transferred";

  private static final int CONNECTION_ID = 1;
  
  // Mocks
  private InputStreamConnector inputStreamConnector;
  private OutputStreamConnector outputStreamConnector;
  private Socket socket;
  private SocketFactory socketFactory;
  private ThreadPoolExecutor threadPoolExecutor;
  private InetAddress localHostAddress;
  private Injector injector;
  private FrameSender frameSender;
  
  // data
  private FrameInfo mockFrame;
  private SocketDataInfo mockSocketDataInfo;
  private LocalConf fakeLocalConf;
  private BlockingQueue<SocketDataInfo> queue;
  
  
  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    fakeLocalConf = new FakeLocalConfGenerator().getFakeLocalConf();
    // 2nd order dependency mocks that isnt important to define behavior
    socket = EasyMock.createNiceMock(Socket.class);
    EasyMock.replay(socket);
    localHostAddress = EasyMock.createNiceMock(InetAddress.class);
    EasyMock.replay(localHostAddress);
    frameSender = EasyMock.createNiceMock(FrameSender.class);
    EasyMock.replay(frameSender);
    queue = new LinkedBlockingQueue<SocketDataInfo>();
    
    // SocketFactory
    socketFactory = EasyMock.createMock(SocketFactory.class);
    socketFactory.createSocket();
    EasyMock.expectLastCall().andReturn(socket);
    EasyMock.replay(socketFactory);
    
    // Input and Output Stream Connectors
    inputStreamConnector = EasyMock.createMock(InputStreamConnector.class);
    inputStreamConnector.setConnectionId(CONNECTION_ID);
    EasyMock.expectLastCall();
    inputStreamConnector.setInputStream(socket.getInputStream());
    EasyMock.expectLastCall();
    inputStreamConnector.setFrameSender(frameSender); 
    EasyMock.expectLastCall();
    inputStreamConnector.setConnectorStateCallback(EasyMock.isA(ConnectionRemover.class));
    EasyMock.expectLastCall();
    inputStreamConnector.setName("Inputconnector-" + CONNECTION_ID);
    EasyMock.expectLastCall();
    EasyMock.replay(inputStreamConnector);
    
    outputStreamConnector = EasyMock.createMock(OutputStreamConnector.class);
    outputStreamConnector.setConnectionId(CONNECTION_ID);
    EasyMock.expectLastCall();
    outputStreamConnector.setOutputStream(socket.getOutputStream());
    EasyMock.expectLastCall();
    outputStreamConnector.setConnectorStateCallback(EasyMock.isA(ConnectionRemover.class));
    EasyMock.expectLastCall();
    outputStreamConnector.setName("Outputconnector-" + CONNECTION_ID);
    EasyMock.expectLastCall();
    outputStreamConnector.getQueue();
    EasyMock.expectLastCall().andReturn(queue);
    EasyMock.replay(outputStreamConnector);
    
    // Injector
    injector = EasyMock.createMock(Injector.class);
    EasyMock.expect(injector.getInstance(InputStreamConnector.class))
        .andReturn(inputStreamConnector);
    EasyMock.expect(injector.getInstance(OutputStreamConnector.class))
        .andReturn(outputStreamConnector);
    EasyMock.replay(injector);
    
    // ThreadPoolExecutor
    threadPoolExecutor = EasyMock.createMock(ThreadPoolExecutor.class);
    threadPoolExecutor.execute(inputStreamConnector);
    EasyMock.expectLastCall();
    threadPoolExecutor.execute(outputStreamConnector);
    EasyMock.expectLastCall();
    EasyMock.replay(threadPoolExecutor);
     
    
  }
  
  public void testDispatchNewConnection() throws Exception {
    mockSocketDataInfo = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.START)
        .build();
    mockFrame = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.SOCKET_DATA)
        .setSequence(1)
        .setPayload(mockSocketDataInfo.toByteString())
        .build();
    
    SocksDataHandler socksDataHandler = new SocksDataHandler(fakeLocalConf,
        socketFactory, localHostAddress, threadPoolExecutor, injector);
    socksDataHandler.setFrameSender(frameSender);
    socksDataHandler.dispatch(mockFrame);
    
    EasyMock.verify(socketFactory, inputStreamConnector, outputStreamConnector, injector, 
        threadPoolExecutor);
  }
  
  public void testDispatchContinuingConnection() throws Exception {
    
    // Setup
    mockSocketDataInfo = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.START)
        .build();
    mockFrame = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.SOCKET_DATA)
        .setSequence(1)
        .setPayload(mockSocketDataInfo.toByteString())
        .build();
    
    SocketDataInfo continuingSocketDataInfo = SocketDataInfo.newBuilder()
        .setConnectionId(CONNECTION_ID)
        .setState(SocketDataInfo.State.CONTINUE)
        .setSegment(ByteString.copyFromUtf8(DATA))
        .build();
    FrameInfo continuingFrame = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.SOCKET_DATA)
        .setSequence(1)
        .setPayload(continuingSocketDataInfo.toByteString())
        .build();
    
    // Execute.
    SocksDataHandler socksDataHandler = new SocksDataHandler(fakeLocalConf,
        socketFactory, localHostAddress, threadPoolExecutor, injector);
    socksDataHandler.setFrameSender(frameSender);
    socksDataHandler.dispatch(mockFrame);
    socksDataHandler.dispatch(continuingFrame);
    
    // Verify
    SocketDataInfo actualSocketDataInfo = queue.take();
    assertEquals(continuingSocketDataInfo, actualSocketDataInfo);
    
    EasyMock.verify(socketFactory, inputStreamConnector, outputStreamConnector, injector, 
        threadPoolExecutor);
  }
  
  public void testDispatchInvalidProtocol() throws Exception {
    mockFrame = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.SOCKET_DATA)
        .setSequence(1)
        .setPayload(ByteString.copyFrom(new byte[] { 0, 0, 0, 0, 0 })) // Invalid pb.
        .build();
    
    SocksDataHandler socksDataHandler = new SocksDataHandler(null, null, null, null, null);
    socksDataHandler.setFrameSender(frameSender);
    try {
      socksDataHandler.dispatch(mockFrame);
      fail("Should have recieved framing exception.");
    } catch (FramingException e) {
      // expected
      return;
    }
     
  }
}
