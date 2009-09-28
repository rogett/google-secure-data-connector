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

import com.google.common.base.Preconditions;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Receiver for SDC Frame protocol.  The SDC Frame protocol uses the {@link FrameInfo} protocol
 * buffer to encapsulate many different connections over the top of one stream.  The FrameReceiver
 * reads from a stream, sanity checks and assembles FrameInfo pbs and dispatches them to the
 * registered {@link Dispatchable}.  It is up to the user of this object to define the
 * {@link Dispatchable} for each {@link FrameInfo.Type} it wishes to handle.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FrameReceiver {

  private static final Logger LOG = Logger.getLogger(FrameReceiver.class);

  static final byte FRAME_START = '*';
  static final byte[] MAGIC = "beefcake".getBytes();
  static final int SEQUENCE_LEN = 8;
  static final int PAYLOAD_LEN = 4;
  static final int HEADER_SIZE = 1 + MAGIC.length + SEQUENCE_LEN + PAYLOAD_LEN;
  static final int MAX_FRAME_SIZE = 1024 * 1024; // 1MB

  // Local fields
  private boolean dispatching;
  private long sequence = 0;
  private ConcurrentMap<FrameInfo.Type, Dispatchable> dispatchMap =
      new ConcurrentHashMap<FrameInfo.Type, Dispatchable>();
  private DataInputStream dataInputStream; // used for byte to primitives conversion.

  // Runtime dependencies
  private InputStream inputStream;
  private AtomicLong byteCounter = new AtomicLong(); // default counter

  /**
   * Reads frames and dispatches them to handlers.  This method does not return and is expected to
   * be used as the listener reading socket input data for frames to dispatch.  Any frame dispatched
   * here will be in the main server loop.  If your {@link Dispatchable} does not fork a new thread
   * and hangs, the entire server will hang.
   *
   * @throws FramingException if any framing errors occur.
   */
  public void startDispatching() throws FramingException {
    dispatching = true; // Turn off synchronous access.
    while (true) {
      dispatch(readFrame());
    }
   }

  /**
   * Reads one frame and returns.  Use this for synchronous calls.  This cannot be called once
   * dispatching has been started.  Dispatching is inherently asynchronous and we cannot
   * guarantee that the frame you will receive is the one you want.
   *
   * @return a single Frame.
   * @throws FramingException if any Framing protocol errors occur.
   */
  public FrameInfo readOneFrame() throws FramingException {
    Preconditions.checkArgument(!dispatching,
        "Cannot call readOneFrame.  Dispatching already started.");
    return readFrame();
  }


  /**
   * Reads one frame and returns.
   *
   * @return a single Frame.
   * @throws FramingException if any Framing protocol errors occur.
   */
  private FrameInfo readFrame() throws FramingException {

    Preconditions.checkNotNull(inputStream, "Must specify inputStream before calling readFrame.");

    try {
      // Read start byte.
      int startIndicator = inputStream.read();
      LOG.debug("Start byte: " + startIndicator);
      if ((byte) startIndicator != FRAME_START) {
        throw new FramingException("Unexpected frame start read");
      }

      // Read and check magic.
      byte[] magic = new byte[MAGIC.length];
      readBytes(magic, MAGIC.length);
      String magicString = new String(magic);
      LOG.debug("Magic: " + magicString);
      if (!new String(magic).equals(new String(MAGIC))) {
        throw new FramingException("Unexpected frame magic read");
      }
      Arrays.equals(magic, MAGIC);

      // Read sequence, verify and increment.
      long readSequence = dataInputStream.readLong();
      LOG.debug("sequence: " + readSequence);
      if (readSequence == sequence) {
        sequence++;
      } else {
        throw new FramingException("Unexpected sequence number. Expected: " + sequence +
            " got:" + readSequence);
      }

      // Read and verify payload length.
      int payloadLength = dataInputStream.readInt();
      LOG.debug("payload length: " + payloadLength);
      if (payloadLength < 0 || payloadLength > MAX_FRAME_SIZE) {
        throw new FramingException("Payload length invalid.");
      }

      // Read in the payload
      byte[] payload = new byte[payloadLength];
      int bytesRead = 0;
      do {
        bytesRead += inputStream.read(payload, bytesRead, payloadLength - bytesRead);
      } while (bytesRead < payloadLength);
      // Update the byte counter with header size and payload length if its specified.
      if (byteCounter != null) {
        byteCounter.addAndGet(HEADER_SIZE + payloadLength);
      }

      LOG.debug("payload: " + payload);
      // Parse the payload into a FrameInfo and return it.
      try {
        FrameInfo frameInfo = FrameInfo.parseFrom(payload);
        LOG.debug("frame:\n" + frameInfo.toString());
        LOG.debug("frame type recevd: " + frameInfo.getType());
        return  frameInfo;
      } catch (InvalidProtocolBufferException e) {
        throw new FramingException(e);
      }
    } catch (IOException e) {
      throw new FramingException("IO Exception on tunnelsocket", e);
    }
  }

  /**
   * Calls {@link InputStream#read(byte[], int, int)} until there is no more bytes left to read.
   * TCP does not guarantee that read will get all the bytes you want.  Therefore, you have to
   * continually read until you get what you desire.
   *
   * @param buffer the buffer to read into.
   * @param amountToRead the amount of bytes to read.
   * @throws IOException if any read errors occur.
   */
  private void readBytes(byte[] buffer, int amountToRead) throws IOException {
    int bytesRead = 0;
    do {
      bytesRead += inputStream.read(buffer, bytesRead, amountToRead - bytesRead);
    } while (bytesRead < amountToRead);
  }

  /**
   * For the given frame find its handler and dispatch the frame there.  This method runs in the
   * same thread as the frame reader.  If your dispatch blocks or goes slow, the entire server
   * will slow down.
   *
   * @param frameInfo the incoming frame.
   * @throws FramingException if any errors occur while processing the frame.
   */
  void dispatch(FrameInfo frameInfo) throws FramingException {

    if (dispatchMap.containsKey(frameInfo.getType())) {
      dispatchMap.get(frameInfo.getType()).dispatch(frameInfo);
    } else {
      LOG.info("Unknown frame received: " + frameInfo);
    }
  }

  public void registerDispatcher(FrameInfo.Type type, Dispatchable dispatchable) {
    dispatchMap.put(type, dispatchable);
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
    dataInputStream = new DataInputStream(inputStream);
  }

  public void setByteCounter(AtomicLong byteCounter) {
    this.byteCounter = byteCounter;
  }
}
