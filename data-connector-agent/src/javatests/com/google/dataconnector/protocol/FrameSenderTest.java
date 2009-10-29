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
 *
 * $Id$
 */
package com.google.dataconnector.protocol;

import com.google.dataconnector.protocol.proto.SdcFrame.AuthorizationInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tests for the {@link FrameSender} class.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FrameSenderTest extends TestCase {

  private ByteArrayOutputStream bos;
  private AuthorizationInfo expectedAuthorizationInfo;
  private FrameInfo expectedFrameInfo1;
  private BlockingQueue<FrameInfo> queue;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    expectedAuthorizationInfo = AuthorizationInfo.newBuilder()
       .setEmail("foo@joonix.net")
       .setPassword("password")
       .build();
    expectedFrameInfo1 = FrameInfo.newBuilder()
       .setType(FrameInfo.Type.AUTHORIZATION)
       .setPayload(expectedAuthorizationInfo.toByteString())
       .build();
  }

  public void testSendRawFrameInfo() throws Exception {
    queue = new LinkedBlockingQueue<FrameInfo>();
    FrameSender frameSender = new FrameSender(queue, null);
    frameSender.setOutputStream(null);
    frameSender.sendFrame(expectedFrameInfo1);
    FrameInfo actualFrameInfo = queue.take();
    assertEquals(expectedFrameInfo1, actualFrameInfo);
  }

  public void testSendFrameTypePayload() throws Exception {
    queue = new LinkedBlockingQueue<FrameInfo>();
    FrameSender frameSender = new FrameSender(queue, null);
    frameSender.setOutputStream(null);
    frameSender.sendFrame(FrameInfo.Type.AUTHORIZATION, expectedAuthorizationInfo.toByteString());
    FrameInfo actualFrameInfo = queue.take();
    assertEquals(expectedFrameInfo1, actualFrameInfo);
  }

  public void testWriteOneFrame() throws Exception {
    bos = new ByteArrayOutputStream();
    FrameSender frameSender = new FrameSender(queue, null);
    frameSender.setOutputStream(bos);
    frameSender.writeOneFrame(expectedFrameInfo1);
    byte[] output = bos.toByteArray();
    int offset = 1;

    byte[] magic = new byte[FrameReceiver.MAGIC.length];
    System.arraycopy(output, offset, magic, 0, FrameReceiver.MAGIC.length);
    assertTrue(Arrays.equals(magic, FrameReceiver.MAGIC));
    offset += FrameReceiver.MAGIC.length;

    byte[] seq = new byte[FrameReceiver.SEQUENCE_LEN];
    System.arraycopy(output, offset, seq, 0, FrameReceiver.SEQUENCE_LEN);
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(seq));
    long actualSequence = dis.readLong();
    assertEquals(0, actualSequence);
    offset += FrameReceiver.SEQUENCE_LEN;

    System.arraycopy(output, offset, seq, 0, FrameReceiver.SEQUENCE_LEN);
    DataInputStream ds = new DataInputStream(new ByteArrayInputStream(seq));
    int actualPayloadLen = ds.readInt();
    assertEquals(expectedFrameInfo1.toByteArray().length, actualPayloadLen);
    offset += FrameReceiver.PAYLOAD_LEN;

    byte[] payload = new byte[actualPayloadLen];
    System.arraycopy(output, offset, payload, 0, actualPayloadLen);
    FrameInfo actualFrameInfo = FrameInfo.parseFrom(payload);
    assertEquals(expectedFrameInfo1, actualFrameInfo);
    
  }
}
