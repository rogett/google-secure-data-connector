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

import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.HealthCheckInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.ServerSuppliedConf;
import com.google.dataconnector.util.ClockUtil;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

/**
 * Handles both sending of health check requests and processing responses.  Health checks are sent
 * as frames to the server.  We implement a {@link Dispatchable} to handle the server responses.
 * If we do not receive a response within the specified timeout we execute the
 * {@link FailCallback} specified at runtime.
 *
 * @author rayc@google.com (Ray Colline)
 * @author vnori@google.com (Vasu Nori)
 */
@Singleton
public class HealthCheckHandler extends Thread implements Dispatchable {

  // amount of time (in sec) to wait for registration info to be received from SDC server
  private static final int TIME_TO_WAIT_FOR_SERVERSUPPLIED_CONF = 5;

  /**
   * Call back interface for when health check fails.
   *
   * @author rayc@google.com (Ray Colline)
   */
  public interface FailCallback {
    public void handleFailure();
  }

  private static final Logger LOG = Logger.getLogger(HealthCheckHandler.class);

  // Injected Dependencies
  private final ClockUtil clock;

  // Runtime Dependencies
  private FrameSender frameSender;
  private FailCallback failCallback;
  private ServerSuppliedConf serverSuppliedConf;

  // Class fields.
  private long lastHealthCheckReceivedStamp = 0;

  @Inject
  public HealthCheckHandler(final ClockUtil clock) {
    this.clock = clock;
  }

  /**
   * Receives the {@link HealthCheckInfo} frame, parses and updates last received timestamp.
   */
  @Override
  public void dispatch(final FrameInfo frameInfo) throws FramingException {

    try {
      HealthCheckInfo.parseFrom(frameInfo.getPayload());
      // Assignment is thread safe.
      lastHealthCheckReceivedStamp = clock.currentTimeMillis();
    } catch (InvalidProtocolBufferException e) {
      throw new FramingException(e);
    }
  }

  @Override
  public void run() {
    Preconditions.checkNotNull(frameSender, "Must define frameSender before starting.");
    Preconditions.checkNotNull(failCallback, "Must define remoteFailSwitch before starting.");

    try {
      // don't start doing anything until the config info is received from the SDC server
      waitUntilServerConfigIsReceived();
      LOG.info("healthcheck thread is started");

      // We start out by setting the health check clock to now giving us 30 seconds to receive our
      // first health check response.
      lastHealthCheckReceivedStamp = clock.currentTimeMillis();

      // We send a healthcheck request every X seconds (configurable in localconf)
      while (true) {
        final HealthCheckInfo hci = HealthCheckInfo.newBuilder()
            .setSource(HealthCheckInfo.Source.CLIENT)
            .setTimeStamp(clock.currentTimeMillis())
            .setType(HealthCheckInfo.Type.REQUEST)
            .build();
        LOG.debug("Sending health check request");
        frameSender.sendFrame(SdcFrame.FrameInfo.Type.HEALTH_CHECK, hci.toByteString());
        sleep(serverSuppliedConf.getHealthCheckWakeUpInterval() * 1000);


        // Every send interval we check to see if we have timed out.  Sending is reliable as
        // it uses a large blocking queue to send frames.  Therefore we can re-use the send
        // thread to verify health check responses.   Java primitives have atomic assignment,
        // and only the dispatcher thread will actually assign the lastHealthCheckReceivedStamp.
        // We call the FailHandler when there is a failure.
        if (clock.currentTimeMillis() - (serverSuppliedConf.getHealthCheckTimeout() * 1000) >
            lastHealthCheckReceivedStamp) {
          LOG.warn("Health check response not received in " +
              (clock.currentTimeMillis() - lastHealthCheckReceivedStamp) + "ms.");
          failCallback.handleFailure();
          return;
        } else {
          LOG.debug("Health check ok, last received " +
              (clock.currentTimeMillis() - lastHealthCheckReceivedStamp) + "ms ago.");
        }
      }
    } catch (InterruptedException e) {
      LOG.warn("Health check sender interrupted. Exiting.");
    }
  }

  private void waitUntilServerConfigIsReceived() throws InterruptedException {
    while (serverSuppliedConf == null) {
      LOG.info("healthcheck config is not yet received from the SDC server. will check again in " +
          TIME_TO_WAIT_FOR_SERVERSUPPLIED_CONF + " sec");
      sleep(TIME_TO_WAIT_FOR_SERVERSUPPLIED_CONF * 1000);
    }
  }

  public void setFrameSender(final FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  public void setFailCallback(final FailCallback failCallback) {
    this.failCallback = failCallback;
  }

  public synchronized void setServerSuppliedConf(final ServerSuppliedConf serverSuppliedConf) {
    this.serverSuppliedConf = serverSuppliedConf;
  }
}
