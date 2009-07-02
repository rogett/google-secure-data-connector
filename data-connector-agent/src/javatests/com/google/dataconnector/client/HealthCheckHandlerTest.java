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

import com.google.dataconnector.client.HealthCheckHandler.Clock;
import com.google.dataconnector.client.HealthCheckHandler.FailCallback;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.HealthCheckInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.ServerSuppliedConf;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Tests for the {@link HealthCheckHandler} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class HealthCheckHandlerTest extends TestCase {
  
  public static long FAKE_TIME_STAMP = 1246405970961L;
  public static String BREAK_MESSAGE = "break out";
  
  private FrameSender frameSender;
  private HealthCheckInfo expectedHci;
  private FrameInfo frameInfo;
  private ServerSuppliedConf serverSuppliedConf;
  
  @Override
  protected void setUp() throws Exception {
    
    expectedHci = HealthCheckInfo.newBuilder()
        .setSource(HealthCheckInfo.Source.SERVER)
        .setType(HealthCheckInfo.Type.RESPONSE)
        .setTimeStamp(FAKE_TIME_STAMP)
        .build();
    frameInfo = FrameInfo.newBuilder()
       .setType(FrameInfo.Type.HEALTH_CHECK)
       .setPayload(expectedHci.toByteString())
       .build();
    
    frameSender = EasyMock.createMock(FrameSender.class);
    frameSender.sendFrame(EasyMock.eq(FrameInfo.Type.HEALTH_CHECK), verifyHci());
    EasyMock.expectLastCall();
    EasyMock.replay(frameSender);
    
    serverSuppliedConf = ServerSuppliedConf.newBuilder()
        .setHealthCheckWakeUpInterval(1)
        .setHealthCheckTimeout(30)
        .build();
  }
  
  public void testDispatchAndNormalCheck() throws Exception {
    
    Clock clock = EasyMock.createMock(Clock.class);
    clock.currentTimeMillis();
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // dispatch
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // before loop
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // creating HCI to send to server
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP + 15000); // checking response time + 15 sec
    // We throw an exception to break out of the loop.  This is called at the log.trace.
    EasyMock.expectLastCall().andThrow(new RuntimeException(BREAK_MESSAGE));
    EasyMock.replay(clock);
    
    // Should have no calls.
    FailCallback failCallback = EasyMock.createMock(FailCallback.class);
    EasyMock.replay(failCallback);
    
    HealthCheckHandler healthCheckHandler = new HealthCheckHandler(clock);
    healthCheckHandler.setFrameSender(frameSender);
    healthCheckHandler.setFailCallback(failCallback);
    healthCheckHandler.setServerSuppliedConf(serverSuppliedConf);
    
    healthCheckHandler.dispatch(frameInfo); // should set the time.
    try {
      healthCheckHandler.run();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().equals(BREAK_MESSAGE));
      EasyMock.verify(clock, frameSender, failCallback);
      return;
    }
    fail("did not recieve runtime exception");
  }
  
  public void testDispatchAndHealthCheckTimeout() throws Exception {
    Clock clock = EasyMock.createMock(Clock.class);
    clock.currentTimeMillis();
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // dispatch
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // before loop
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP); // creating HCI to send to server
    // Setup health check fail.
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP + 45000); // checking response time + 45 sec
    EasyMock.expectLastCall().andReturn(FAKE_TIME_STAMP + 45000); // log message.
    EasyMock.replay(clock);
    
    // Call back handler should fire.
    FailCallback failCallback = EasyMock.createMock(FailCallback.class);
    failCallback.handleFailure();
    EasyMock.expectLastCall();
    EasyMock.replay(failCallback);
    
    HealthCheckHandler healthCheckHandler = new HealthCheckHandler(clock);
    healthCheckHandler.setFrameSender(frameSender);
    healthCheckHandler.setFailCallback(failCallback);
    healthCheckHandler.setServerSuppliedConf(serverSuppliedConf);
    
    healthCheckHandler.dispatch(frameInfo); // should set the time.
    healthCheckHandler.run();
    EasyMock.verify(clock, frameSender, failCallback);
  }
  
   /**
    * Access method to setup the HCI matcher.
    */
  public ByteString verifyHci() {
    EasyMock.reportMatcher(new HealthCheckInfoMatcher()); 
    return null;
  }
  
   /**
    * Matcher to ensure a properly formatted HCI is sent to the server.
    */
  public static class HealthCheckInfoMatcher implements IArgumentMatcher {
    
   @Override
    public void appendTo(StringBuffer error) {
       error.append("unexpected HealthCheckInfo supplied");
    } 
   
   @Override
    public boolean matches(Object actual) {
      if (!(actual instanceof ByteString)) {
        return false;
      }
      ByteString actualPayload = (ByteString) actual;
      try {
        HealthCheckInfo actualHci = HealthCheckInfo.parseFrom(actualPayload);
        if (actualHci.getSource() != HealthCheckInfo.Source.CLIENT) {
          return false;
        } else if (actualHci.getType() != HealthCheckInfo.Type.REQUEST) {
          return false;
        } else if (actualHci.getTimeStamp() != FAKE_TIME_STAMP) {
          return false;
        }
        return true;
      } catch (InvalidProtocolBufferException e) {
        return false;
      }
    }
  }

}
