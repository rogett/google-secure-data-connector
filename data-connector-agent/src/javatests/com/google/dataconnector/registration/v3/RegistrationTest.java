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
package com.google.dataconnector.registration.v3;

import com.google.dataconnector.client.ResourceRuleProcessor;
import com.google.dataconnector.protocol.FrameReceiver;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.RegistrationInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo.Type;
import com.google.dataconnector.registration.v3.Registration;
import com.google.dataconnector.registration.v3.RegistrationRequest;
import com.google.dataconnector.registration.v3.ResourceRule;
import com.google.dataconnector.registration.v3.ResourceRuleUtil;
import com.google.dataconnector.registration.v3.testing.FakeResourceRuleConfig;
import com.google.dataconnector.util.RegistrationException;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;

import java.util.List;

/**
 * Tests for the {@link Registration} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationTest extends TestCase {
  
  private FrameSender frameSender;
  private FrameReceiver frameReceiver;
  private List<ResourceRule> resourceRules;
  private RegistrationRequest registrationRequest;
  private ResourceRuleProcessor mockProcessResourceRules;
  
  private RegistrationInfo expectedRegistrationInfo;
  private RegistrationInfo expectedRegistrationResponseInfo;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    frameSender = EasyMock.createMock(FrameSender.class);
    frameReceiver = EasyMock.createMock(FrameReceiver.class);
    
    resourceRules = new FakeResourceRuleConfig().getFakeConfigResourceRules();
    registrationRequest = new RegistrationRequest(
        new ResourceRuleUtil(new XmlUtil(), new BeanUtil()));
    registrationRequest.populateFromResources(resourceRules);
    
    expectedRegistrationInfo = RegistrationInfo.newBuilder()
        .setXml(registrationRequest.toJson().toString())
        .build();
    
    frameSender.sendFrame(EasyMock.eq(FrameInfo.Type.REGISTRATION), 
        equalXml(expectedRegistrationInfo));
    EasyMock.expectLastCall();
    
    expectedRegistrationResponseInfo = RegistrationInfo.newBuilder()
       .setResult(RegistrationInfo.ResultCode.OK)
       .build();
    EasyMock.replay(frameSender);
    
    frameReceiver.readOneFrame();
    EasyMock.expectLastCall().andReturn(FrameInfo.newBuilder()
        .setType(Type.REGISTRATION)
        .setPayload(expectedRegistrationResponseInfo.toByteString())
        .build());
    EasyMock.replay(frameReceiver);

    mockProcessResourceRules = EasyMock.createMock(ResourceRuleProcessor.class);
    EasyMock.expect(mockProcessResourceRules.getResourceRules()).andReturn(resourceRules);
    EasyMock.replay(mockProcessResourceRules);
  }
  
  public void testSuccessfulRegistration() throws Exception {
    // Execute
    Registration registration = new Registration(registrationRequest, mockProcessResourceRules);
    // Will throw exceptions if there are any errors.
    registration.register(frameReceiver, frameSender);
    
    // Verify
    EasyMock.verify(frameSender, frameReceiver);
  }
  
  public void testFailedRegistration() throws Exception {
    
    // Setup
    RegistrationInfo expectedRegistrationResponseInfo = RegistrationInfo.newBuilder()
       .setResult(RegistrationInfo.ResultCode.FAILED)
       .build();
    
    FrameReceiver frameReceiver = EasyMock.createMock(FrameReceiver.class);
    frameReceiver.readOneFrame();
    EasyMock.expectLastCall().andReturn(FrameInfo.newBuilder()
        .setType(Type.REGISTRATION)
        .setPayload(expectedRegistrationResponseInfo.toByteString())
        .build());
    EasyMock.replay(frameReceiver);
    
    // Execute
    Registration registration = new Registration(registrationRequest, mockProcessResourceRules);
    // Will throw exceptions if there are any errors.
    try {
      registration.register(frameReceiver, frameSender);
      fail("Did not receive registration exception.");
    } catch (RegistrationException e) {
      assertTrue(e.getMessage().startsWith("Registration failed"));
      EasyMock.verify(frameSender, frameReceiver);
      return; 
    }
  }
  
  public void testFailedRegistrationFrameError() throws Exception {
    // Setup
    FrameReceiver frameReceiver = EasyMock.createMock(FrameReceiver.class);
    frameReceiver.readOneFrame();
    EasyMock.expectLastCall().andThrow(new FramingException("Frame read error"));
    EasyMock.replay(frameReceiver);
    
    // Execute
    Registration registration = new Registration(registrationRequest, mockProcessResourceRules);
    // Will throw exceptions if there are any errors.
    try {
      registration.register(frameReceiver, frameSender);
      fail("Did not receive registration exception.");
    } catch (RegistrationException e) {
      assertTrue(e.getCause() instanceof FramingException);
      EasyMock.verify(frameSender, frameReceiver);
      return; 
    }
  }
  
  /**
   * EasyMock "equals" method for RegistrationInfoMatcher.
   * 
   * @param expectedRegInfo the expected registration info to be compared.
   * @return null to satisfy interface.
   */
  private ByteString equalXml(RegistrationInfo expectedRegInfo) {
    EasyMock.reportMatcher(new RegistrationInfoMatcher(expectedRegInfo));
    return null;
  }
  
  /**
   * Matcher that compares the XML value of the registration request to see if they are equal.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  private static class RegistrationInfoMatcher implements IArgumentMatcher {

    private RegistrationInfo expected;
    
    public RegistrationInfoMatcher(RegistrationInfo expected) {
      this.expected = expected;
    }
    
    @Override
    public void appendTo(StringBuffer buffer) {
      buffer.append("expected: " + expected).toString();
    }

    @Override
    public boolean matches(Object actual) {
      if (!(actual instanceof ByteString)) {
        return false;
      }
      ByteString actualByteString = (ByteString) actual;
      RegistrationInfo actualRegInfo;
      try {
        actualRegInfo = RegistrationInfo.parseFrom(actualByteString);
      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
        return false;
      }
      return expected.getXml().equals(actualRegInfo.getXml());
    }
  }
}
