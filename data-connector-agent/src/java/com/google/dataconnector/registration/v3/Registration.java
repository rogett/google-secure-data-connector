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
import com.google.dataconnector.protocol.proto.SdcFrame.ServerSuppliedConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * Handles registration for SDC agent.  Prepares the resource rules into a {@link RegistrationInfo}
 * protocol buffer and sends it to the server.  It awaits for response.  It is currently written
 * to be synchronous and happen before dispatching starts.  
 * 
 * TODO(rayc) Make this a dispatchable and allow updating of registration info at any point.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Registration {
  
  private static final Logger LOG = Logger.getLogger(Registration.class);
  
  private RegistrationRequest registrationRequest;
  private ResourceRuleProcessor resourceRuleProcessor;

  @Inject
  public Registration(RegistrationRequest registrationRequest, 
      ResourceRuleProcessor resourceRuleProcessor) {
    this.registrationRequest = registrationRequest;
    this.resourceRuleProcessor = resourceRuleProcessor;
  }
   
  /**
   * With the given {@link FrameReceiver} and {@link FrameSender} perform registration
   * synchronously.
   * 
   * @param frameReceiver the frame receiver to use to receive registration response.
   * @param frameSender the frame sender to use to send the registration response.
   * @returns the server provided configuration.
   * @throws RegistrationException if registration fails or there is a communication error.
   */
  public ServerSuppliedConf register(FrameReceiver frameReceiver, FrameSender frameSender) 
      throws RegistrationException {
    
    try {
      // TODO(rayc) Remove need for registrationRequest v2 stuff.  Directly use the protobuf.
      // Prepare reg request.
      registrationRequest.populateFromResources(resourceRuleProcessor.getResourceRules());
      RegistrationInfo registrationFrame = RegistrationInfo.newBuilder()
          .setXml(registrationRequest.toJson().toString()).build();
      
      // Send frame.
      frameSender.sendFrame(FrameInfo.Type.REGISTRATION, registrationFrame.toByteString());
      
      // Wait for response.
      RegistrationInfo responseRegistrationFrame = 
          RegistrationInfo.parseFrom(frameReceiver.readOneFrame().getPayload());
      
      if (responseRegistrationFrame.getResult() != RegistrationInfo.ResultCode.OK) {
        throw new RegistrationException("Registration failed: " + 
            responseRegistrationFrame.getStatusMessage());
      }
      
      LOG.info("registration successful. Received config info from the SDC server\n" +
          responseRegistrationFrame.getServerSuppliedConf().toString());
      return responseRegistrationFrame.getServerSuppliedConf();
    } catch (JSONException e) {
      throw new RegistrationException(e);
    } catch (ResourceException e) {
      throw new RegistrationException(e);
    } catch (FramingException e) {
      throw new RegistrationException(e);
    } catch (InvalidProtocolBufferException e) {
      throw new RegistrationException(e);
    }
  }
}
    
  
