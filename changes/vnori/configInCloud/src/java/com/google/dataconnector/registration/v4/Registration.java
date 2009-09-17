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
package com.google.dataconnector.registration.v4;

import com.google.dataconnector.client.HealthCheckHandler;
import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.RegistrationRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.RegistrationResponse;
import com.google.dataconnector.protocol.proto.SdcFrame.ResourceKey;
import com.google.dataconnector.protocol.proto.SdcFrame.ServerSuppliedConf;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.registration.v4.ResourceRuleUrlUtil;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.HealthCheckRequestHandler;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.SdcKeysManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

/**
 * Handles registration for SDC agent.  contains 2 methods
 * 1. method to prepares the resource rules into a
 * {@link RegistrationRequest} protobuf and send it to the SDC server.
 *
 * 2. method to parse the response from the SDC server.
 *
 * @author rayc@google.com (Ray Colline)
 * @author vnori@google.com (Vasu Nori)
 */
@Singleton
public class Registration implements Dispatchable {

  private static final Logger LOG = Logger.getLogger(Registration.class);

  // injected dependencies
  private final LocalConf localConf;
  private final HealthCheckRequestHandler healthCheckRequestHandler;
  private final FileUtil fileUtil;
  private final ResourceRuleUrlUtil resourceRuleUrlUtil;
  private final SdcKeysManager sdcKeysManager;
  private final HealthCheckHandler healthCheckHandler;
  private final ResourceRuleParser resourceRuleParser;

  @Inject
  public Registration(LocalConf localConf, HealthCheckRequestHandler healthCheckRequestHandler,
      FileUtil fileUtil, ResourceRuleUrlUtil resourceRuleUrlUtil, SdcKeysManager sdcKeysManager,
      HealthCheckHandler healthCheckHandler, ResourceRuleParser resourceRuleParser) {
    this.localConf = localConf;
    this.healthCheckRequestHandler = healthCheckRequestHandler;
    this.fileUtil = fileUtil;
    this.resourceRuleUrlUtil = resourceRuleUrlUtil;
    this.sdcKeysManager = sdcKeysManager;
    this.healthCheckHandler = healthCheckHandler;
    this.resourceRuleParser = resourceRuleParser;
  }

  /**
   * Gets called by the frame receiver when a Registration frame is received.
   * SDC server must have sent {@link RegistrationResponse} data.
   * process the response recvd.
   *
   * @throws FramingException if any IO errors with plumbing, unparsable frames, or frames in
   * a bad state.
   */
  @Override
  public void dispatch(FrameInfo frameInfo) throws FramingException {
    try {
      processRegistrationResponse(frameInfo);
    } catch (RegistrationException e) {
      // this re-throws a registration exception - but caller of this method catches it and
      // throws it as ConnectionException - which is what RegistrationException is.
      // fix this convoluted exception chaining.
      throw new FramingException(e);
    }
  }

  /**
   * Send registration info to SDC server.
   *
   * @param frameSender the frame sender to use to send the registration response.
   * @returns the server provided configuration.
   * @throws RegistrationException if registration fails or there is a communication error.
   */
  public void sendRegistrationInfo(FrameSender frameSender) throws RegistrationException {
    try {
      // prepare registration request
      RegistrationRequest.Builder regRequestBuilder = RegistrationRequest.newBuilder()
          .setHealthCheckPort(healthCheckRequestHandler.getPort())
          .setAgentId(localConf.getAgentId())
          .setSocksServerPort(localConf.getSocksServerPort());

      // are there any healthcheckgadget users defined?
      List<String> healthCheckGadgetUsersList = getHealthCheckGadgetUsers();
      if (healthCheckGadgetUsersList != null) {
        regRequestBuilder.addAllHealthCheckGadgetUser(healthCheckGadgetUsersList);
      }

      // set resources xml in the protobuf
      regRequestBuilder.setResourcesXml(fileUtil.readFile(localConf.getRulesFile()));

      // set resource keys in the protobuf
      List<ResourceKey> resourceKeyList = createResourceKeys(regRequestBuilder);
      regRequestBuilder.addAllResourceKey(resourceKeyList);

      // finalize the building of the RegRequest
      RegistrationRequest regRequest = regRequestBuilder.build();

      // Send frame.
      LOG.info("Sending resources info\n" + regRequest.toString());
      frameSender.sendFrame(FrameInfo.Type.REGISTRATION, regRequest.toByteString());

      // store the resource keys
      sdcKeysManager.storeSecretKeys(regRequest.getResourceKeyList());
    } catch (IOException e) {
      throw new RegistrationException(e);
    }
  }

  /**
   * return list of healthcheckgadget users declared in LocalConf.
   */
  private List<String> getHealthCheckGadgetUsers() {
    String healthCheckGadgetUsers = localConf.getHealthCheckGadgetUsers();
    List<String> healthCheckGadgetUsersList = null;
    if (healthCheckGadgetUsers != null && healthCheckGadgetUsers.trim().length() > 0) {
      healthCheckGadgetUsersList = new ArrayList<String>();
      String[] users = healthCheckGadgetUsers.split(",");
      for (String user : users) {
        if (user.trim().length() > 0) {
          healthCheckGadgetUsersList.add(user.trim());
        }
      }
    }
    return healthCheckGadgetUsersList;
  }

  /**
   * create resource secretkeys for all URLs and return the list
   */
  private List<ResourceKey> createResourceKeys(RegistrationRequest.Builder regRequestBuilder)
      throws RegistrationException {
    List<ResourceKey> resourceKeyList = new ArrayList<ResourceKey>();
    try {
      List<String> urlList = resourceRuleParser.parseResourcesFile(localConf.getRulesFile(),
          localConf.getAgentId());

      // create keys for all urls in the list received above
      for (String u : urlList) {
        resourceKeyList.add((ResourceKey.newBuilder()
            .setIp(resourceRuleUrlUtil.getHostnameFromRule(u))
            .setPort(resourceRuleUrlUtil.getPortFromRule(u))
            .setKey(new Random().nextLong()).build()));
      }
      // create an additional resourceKey for the healthcheck resource rule
      resourceKeyList.add(ResourceKey.newBuilder()
          .setIp("localhost")
          .setPort(healthCheckRequestHandler.getPort())
          .setKey(new Random().nextLong())
          .build());
    } catch (FileNotFoundException e) {
      throw new RegistrationException(e);
    } catch (XMLStreamException e) {
      throw new RegistrationException(e);
    } catch (FactoryConfigurationError e) {
      throw new RegistrationException(e);
    } catch (ResourceUrlException e) {
      throw new RegistrationException(e);
    }
    return resourceKeyList;
  }

  /**
   * process the registration response received from the SDC server
   */
  private void processRegistrationResponse(FrameInfo frameInfo) throws RegistrationException {
    try {
      RegistrationResponse regResponse = RegistrationResponse.parseFrom(frameInfo.getPayload());
      if (regResponse.getResult() != RegistrationResponse.ResultCode.OK) {
        throw new RegistrationException("Registration failed: " +
            regResponse.getStatusMessage());
      }
      LOG.debug("Received response to resource registration request\n" + regResponse.toString());

      if (regResponse.hasServerSuppliedConf()) {
        ServerSuppliedConf serverSuppliedConf = regResponse.getServerSuppliedConf();
        LOG.info("registration successful. Received config info from the SDC server\n" +
            regResponse.getServerSuppliedConf().toString());
        healthCheckHandler.setServerSuppliedConf(serverSuppliedConf);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new RegistrationException(e);
    }
  }
}