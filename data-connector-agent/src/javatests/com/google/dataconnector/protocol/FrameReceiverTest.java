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

import com.google.dataconnector.protocol.proto.SdcFrame.AuthorizationInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for the {@link FrameReceiver} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class FrameReceiverTest extends TestCase {

  private ByteArrayOutputStream bos;
  private ByteArrayInputStream bis;
  private AuthorizationInfo mockAuthorizationInfo;
  private FrameInfo expectedFrameInfo1;
  private FrameInfo expectedFrameInfo2;
  
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    long sequence = 0;
    mockAuthorizationInfo = AuthorizationInfo.newBuilder()
       .setEmail("foo@joonix.net")
       .setPassword("password")
       .build();
    expectedFrameInfo1 = FrameInfo.newBuilder()
       .setType(FrameInfo.Type.AUTHORIZATION)
       .setSequence(sequence)
       .setPayload(mockAuthorizationInfo.toByteString())
       .build();
       
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*');
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(sequence);
    dataOutputStream.writeInt(expectedFrameInfo1.toByteArray().length);
    bos.write(expectedFrameInfo1.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    sequence++;
    expectedFrameInfo2 = FrameInfo.newBuilder().mergeFrom(expectedFrameInfo1)
        .setSequence(sequence)
        .build();
    
    // Frame 2
    bos.write('*');
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(sequence);
    dataOutputStream.writeInt(expectedFrameInfo2.toByteArray().length);
    bos.write(expectedFrameInfo2.toByteArray());
    
    bis = new ByteArrayInputStream(bos.toByteArray());
  }
  
  public void testReadOneFrame() throws Exception {
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
    assertEquals(expectedFrameInfo1, actualFrameInfo);
  }
  
  public void testDispatching() throws Exception {
    FrameReceiver frameReceiver = new FrameReceiver();
    MockDispatchable dispatchable = new MockDispatchable();
    frameReceiver.registerDispatcher(FrameInfo.Type.AUTHORIZATION, dispatchable);
    frameReceiver.dispatch(expectedFrameInfo1);
    frameReceiver.dispatch(expectedFrameInfo2);
    assertEquals(expectedFrameInfo1, dispatchable.getReceivedFrames().get(0));
    assertEquals(expectedFrameInfo2, dispatchable.getReceivedFrames().get(1));
  }
  
  public void testCounter() throws Exception {
    AtomicLong actualCounter = new AtomicLong();
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    frameReceiver.setByteCounter(actualCounter);
    FrameInfo actualFrameInfo1 = frameReceiver.readOneFrame();
    FrameInfo actualFrameInfo2 = frameReceiver.readOneFrame();
    assertEquals(actualCounter.get(), bos.toByteArray().length);
  }
  
  public void testBadPayloadNumber() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*'); 
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(0);
    dataOutputStream.writeInt(13434343);
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("Payload length"));
      return;
    }
  }
  
  public void testBadPayloadBytes() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*'); 
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(0);
    bos.write("jibber jabber".getBytes());
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("Payload length"));
      return;
    }
  }
  
  public void testBadSequenceNumber() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*'); 
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(100000L);
    dataOutputStream.writeInt(expectedFrameInfo1.toByteArray().length);
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("sequence number"));
      return;
    }
  }
  
  public void testBadSequenceBytes() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*'); 
    bos.write(FrameReceiver.MAGIC);
    bos.write("not a sequence".getBytes());
    dataOutputStream.writeInt(expectedFrameInfo1.toByteArray().length);
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("sequence number"));
      return;
    }
  }
  
  public void testBadFrameMagic() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('*'); 
    bos.write("So Not BEEFCAKE".getBytes()); // INVALID MAGIC
    dataOutputStream.writeLong(0);
    dataOutputStream.writeInt(expectedFrameInfo1.toByteArray().length);
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("frame magic read"));
      return;
    }
  }
  
  public void testBadFrameStart() throws Exception {
    bos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(bos);
    // Frame 1
    bos.write('d'); // INVALID start
    bos.write(FrameReceiver.MAGIC);
    dataOutputStream.writeLong(0);
    dataOutputStream.writeInt(expectedFrameInfo1.toByteArray().length);
    bos.write(expectedFrameInfo1.toByteArray());
    bis = new ByteArrayInputStream(bos.toByteArray());
    
    // increment sequence and make new frameInfo with new sequence.
    FrameReceiver frameReceiver = new FrameReceiver();
    frameReceiver.setInputStream(bis);
    try {
      FrameInfo actualFrameInfo = frameReceiver.readOneFrame();
      fail("did not receive framing exception");
    } catch (FramingException e) {
      assertTrue(e.getMessage().contains("frame start read"));
      return;
    }
  }
  
  public class MockDispatchable implements Dispatchable {
    
    private List<FrameInfo> receivedFrames = new ArrayList<FrameInfo>();
    
    @SuppressWarnings("unused")
    @Override
    public void dispatch(FrameInfo frameInfo) throws FramingException {
      receivedFrames.add(frameInfo);
    }
    
    public List<FrameInfo> getReceivedFrames() {
      return receivedFrames;
    }
  }
  
  
}
