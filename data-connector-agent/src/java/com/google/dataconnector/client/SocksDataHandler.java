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
package com.google.dataconnector.client;

import com.google.dataconnector.protocol.ConnectorStateCallback;
import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.InputStreamConnector;
import com.google.dataconnector.protocol.OutputStreamConnector;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.dataconnector.util.LocalConf;

import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.SocketFactory;

/**
 * Handler for all incoming socket connections from the cloud.  Listens for new
 * {@link SocketDataInfo} frames and handles plumbing connections to the local socks server.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class SocksDataHandler implements Dispatchable {

  public static Logger LOG = Logger.getLogger(SocksDataHandler.class);

  // Injected dependencies
  private final SocketFactory socketFactory;
  private final LocalConf localConf;
  private final InetAddress localHostAddress;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final Injector injector;

  // Runtime dependencies
  private FrameSender frameSender;

  // Local fields
  private ConcurrentMap<Integer, BlockingQueue<SocketDataInfo>> outputQueueMap;

  public interface ConnectionStateUpdatable {
     public void removeConnection(int connectionId);
  }

  @Inject
  public SocksDataHandler(LocalConf localConf, SocketFactory socketFactory,
      @Named("localhost") InetAddress localHostAddress, ThreadPoolExecutor threadPoolExecutor,
      Injector injector) {

    outputQueueMap = new ConcurrentHashMap<Integer, BlockingQueue<SocketDataInfo>>();
    this.localConf = localConf;
    this.socketFactory = socketFactory;
    this.localHostAddress = localHostAddress;
    this.threadPoolExecutor = threadPoolExecutor;
    this.injector = injector;
  }

  /**
   * Gets called by the frame receiver when a SocketDataInfo frame is received.  Depending
   * on the frame STATE, it will plumb a new connection to the socks server or send data
   * to an existing connection.
   *
   * @throws FramingException if any IO errors with plumbing, unparsable frames, or frames in
   * a bad state.
   */
  @Override
  public void dispatch(FrameInfo frameInfo) throws FramingException {
    Preconditions.checkNotNull(frameSender, "Must define frameSender before calling dispatch");
    try {
      SocketDataInfo socketDataInfo = SocketDataInfo.parseFrom(frameInfo.getPayload());
      int connectionId = (int) socketDataInfo.getConnectionId();

      // Handle incoming start request.
      if (socketDataInfo.getState() == SocketDataInfo.State.START) {
        LOG.info("Starting new connection. ID " + connectionId);
        Socket socket = socketFactory.createSocket();
        socket.connect(new InetSocketAddress(localHostAddress, localConf.getSocksServerPort()));

        ConnectionRemover connectionRemoverCallback = new ConnectionRemover();

        // TODO(rayc) Create a pool of connectors instead of making a new instance each time.
        InputStreamConnector inputStreamConnector =
            injector.getInstance(InputStreamConnector.class);
        inputStreamConnector.setConnectionId(connectionId);
        inputStreamConnector.setInputStream(socket.getInputStream());
        inputStreamConnector.setFrameSender(frameSender);
        inputStreamConnector.setConnectorStateCallback(connectionRemoverCallback);
        inputStreamConnector.setName("Inputconnector-" + connectionId);

        // TODO(rayc) Create a pool of connectors instead of making a new instance each time.
        OutputStreamConnector outputStreamConnector =
            injector.getInstance(OutputStreamConnector.class);
        outputStreamConnector.setConnectionId(connectionId);
        outputStreamConnector.setOutputStream(socket.getOutputStream());
        outputStreamConnector.setConnectorStateCallback(connectionRemoverCallback);
        outputStreamConnector.setName("Outputconnector-" + connectionId);
        outputQueueMap.put(connectionId, outputStreamConnector.getQueue());

        // Start threads
        threadPoolExecutor.execute(inputStreamConnector);
        threadPoolExecutor.execute(outputStreamConnector);
        LOG.debug("active thread count = " + Thread.activeCount());
      // Deal with continuing connections or close connections.
      } else if (socketDataInfo.getState() == SocketDataInfo.State.CONTINUE ||
          socketDataInfo.getState() == SocketDataInfo.State.CLOSE) {
        if (outputQueueMap.containsKey((int) socketDataInfo.getConnectionId())) {
          outputQueueMap.get(connectionId).put(socketDataInfo);
        }
      // Unknown states.
      } else {
        throw new FramingException("Unknown State: " + socketDataInfo.getState() +
            " received while dispatching");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new FramingException(e);
    } catch (IOException e) {
      // TODO(rayc) Later on do something more intelligent such as reject this request not kill
      // the tunnel.
      throw new FramingException(e);
    } catch (InterruptedException e) {
      throw new FramingException(e);
    } catch (RejectedExecutionException e){
      LOG.warn("Out of threads, waiting for some to free up.  Total active " +
          threadPoolExecutor.getActiveCount() + " queue Map entries" + outputQueueMap.size());
      throw new FramingException("Out of threads!");
    }
  }

  public void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  /**
   * Provides callback for InputStreamConnector and OutputStreamConnector for when connection state
   * changes on input or output streams.
   *
   * @author rayc@google.com (Ray Colline)
   */
  public class ConnectionRemover implements ConnectorStateCallback {

    /**
     * Removes connection from the queueMap so its no longer tracked.
     */
    @Override
    public void close(int connectionId) {
      // We never know if the input or output side will detect closure first.
      // We defensively call from both sides.  In the event we are called twice we check to see
      // if we have already cleaned up.
      if (outputQueueMap.containsKey(connectionId)) {
        // We tell the output thread to give up by placing a final CLOSE SocketData.
        outputQueueMap.get(connectionId).add(SocketDataInfo.newBuilder()
            .setState(SocketDataInfo.State.CLOSE)
            .setConnectionId(connectionId).build());
        outputQueueMap.remove(connectionId);
      }
    }
  }
}