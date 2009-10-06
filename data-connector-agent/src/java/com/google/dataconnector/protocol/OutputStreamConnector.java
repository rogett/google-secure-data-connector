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
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

/**
 * Converts {@link SocketDataInfo} into bytes on an output stream for use with streaming
 * sockets over the SDC Frame Protocol.  A queue is watched to wait for SocketDataInfo protocol
 * buffers and when one is received, {@link OutputStreamConnector} writes its segment contents
 * onto the wire.  If a CLOSE SocketDataInfo is received, the underlying output stream is closed
 * and the {@link ConnectorStateCallback#close(int)} is fired.
 *
 * @author rayc@google.com (Ray Colline)
 *
 */
public class OutputStreamConnector extends Thread {

  private static final Logger LOG = Logger.getLogger(OutputStreamConnector.class);

  // runtime dependencies
  private OutputStream outputStream;
  private long connectionId;
  private ConnectorStateCallback connectorStateCallback;

  // local fields
  private BlockingQueue<SocketDataInfo> queue;

  @Inject
  public OutputStreamConnector(BlockingQueue<SocketDataInfo> queue) {
    this.queue = queue;
  }

  /**
   * Watches the {@link SocketDataInfo} queue and writes any available frames to the output stream.
   * If the {@link SocketDataInfo} indicates CLOSE, the output stream is closed and the
   * {@link ConnectorStateCallback#close} is fired.
   */
  @Override
  public void run() {
    Preconditions.checkNotNull(outputStream, "must set outputStream before calling start()");
    Preconditions.checkNotNull(connectionId, "must set connectionId before calling start()");

    try {
      while (true) {
        SocketDataInfo socketDataInfo = queue.take();

        if (socketDataInfo.getState() == SocketDataInfo.State.CLOSE) {
          LOG.debug("Closing connection " + connectionId);
          outputStream.close();
          break;
        } else if (socketDataInfo.getState() == SocketDataInfo.State.CONTINUE) {
          LOG.debug("frame = " + socketDataInfo.toString());
          outputStream.write(socketDataInfo.getSegment().toByteArray());
        }
      }
    } catch (InterruptedException e) {
      LOG.info("Interrupted while waiting for socket data frames");
    } catch (IOException e) {
      // TODO(rayc) Later on do something more intelligent such as reject this request not kill
      // the tunnel.
      LOG.debug("IO error", e);
    } finally {
      connectorStateCallback.close(connectionId);
      LOG.debug("Stopping output for ID:" + connectionId + "active thread count:" +
          Thread.activeCount());
    }
  }

  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void setConnectionId(final long connectionId) {
    this.connectionId = connectionId;
  }

  public BlockingQueue<SocketDataInfo> getQueue() {
    return queue;
  }

  public void setConnectorStateCallback(ConnectorStateCallback connectorStateCallback) {
    this.connectorStateCallback = connectorStateCallback;
  }
}
