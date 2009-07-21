/* Copyright 2008 Google Inc.
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

import com.google.dataconnector.protocol.proto.SdcFrame.ResourceKey;
import com.google.dataconnector.registration.v3.ResourceException;
import com.google.dataconnector.registration.v3.ResourceRule;
import com.google.dataconnector.registration.v3.ResourceRuleUtil;
import com.google.dataconnector.registration.v3.ResourceRuleValidator;
import com.google.dataconnector.registration.v3.SocketInfo;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.HealthCheckRequestHandler;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.SdcKeysManager;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** this code is going to go away pretty soon. so
 * it is light on comments and have NO unittest for it.
 * 
 * @author vnori@google.com (Your Name Here)
 */
@Singleton
public class ResourceRuleProcessor {

  private static final Logger LOG = Logger.getLogger(ResourceRuleProcessor.class);
  
  private final LocalConf localConf;
  private final ResourceRuleUtil resourceRuleUtil;
  private final HealthCheckRequestHandler healthCheckRequestHandler;
  private final SdcKeysManager sdcKeysManager;
  
  // processed resource rules stored here
  private List<ResourceRule> resourceRules = null;
  
  @Inject
  public ResourceRuleProcessor(LocalConf localConf,
      ResourceRuleUtil resourceRuleUtil, 
      HealthCheckRequestHandler healthCheckRequestHandler,
      SdcKeysManager sdcKeysManager) {
    this.localConf = localConf;
    this.resourceRuleUtil = resourceRuleUtil;
    this.healthCheckRequestHandler = healthCheckRequestHandler;
    this.sdcKeysManager = sdcKeysManager;
  }
  
  /**
   * Provider method that returns a list of {@link ResourceRule} that is read from the file 
   * system location pointed to by the {@link LocalConf} bean.  It also prepares the resource
   * rules for runtime.
   * 
   * @return the created List of resources.
   * @throws ResourceException 
   */
  public List<ResourceRule> getResourceRules() throws ResourceException {
    Preconditions.checkContentsNotNull(resourceRules, 
        "validate() should already have been called before this method is called");    
    return resourceRules;
  }
  
  /**
   * returns true if the resourcesfiles is parsed without any errors. false otherwise.
   */
  void process() throws ResourceException {
    // process rules
    try {
      resourceRules = resourceRuleUtil.getResourceRules(
          new FileUtil().readFile(localConf.getRulesFile()));
      
      // Add System resource rules to the list
      LOG.info("Adding system resource rules");
      List<ResourceRule> systemRules = resourceRuleUtil.createSystemRules(localConf.getUser(),
          localConf.getDomain(), localConf.getAgentId(), healthCheckRequestHandler.getPort(),
          localConf.getHealthCheckGadgetUsers());
      for (ResourceRule r: systemRules) {
        resourceRules.add(r);
      }
      
      // Validate Resource Rules
      ResourceRuleValidator resourceRuleValidator = new ResourceRuleValidator();
      resourceRuleValidator.validate(resourceRules);
      
      // Remove all the rules in the config that arent for this client.
      resourceRuleUtil.getThisClientRulesAndSetId(resourceRules, localConf.getAgentId());
      
      // Set the socks server port for the rules based on the local configuration.
      resourceRuleUtil.setSocksServerPort(resourceRules, localConf.getSocksServerPort());
      resourceRuleUtil.setSecretKeys(resourceRules);
      
      /** store all the keys in SdcKeys Manager */    
      List<ResourceKey> resourceKeyList = new ArrayList<ResourceKey>();
      for (ResourceRule resourceRule : resourceRules) {
        if (resourceRule.getUrl().startsWith(ResourceRule.SOCKETID)) {
          SocketInfo socketInfo;
          try {
            socketInfo = new SocketInfo(resourceRule.getUrl());
          } catch (ResourceException e) {
            throw new RuntimeException("Invalid Socket Pattern : entry.getPattern()");
          }
          resourceKeyList.add(ResourceKey.newBuilder()
              .setKey(resourceRule.getSecretKey())
              .setIp(socketInfo.getHostAddress())
              .setPort(socketInfo.getPort()).build());
        } else if (resourceRule.getUrl().startsWith(ResourceRule.HTTPID) || 
            (resourceRule.getUrl().startsWith(ResourceRule.HTTPSID))) {
          resourceKeyList.add(ResourceKey.newBuilder()
              .setKey(resourceRule.getSecretKey())
              .setIp(resourceRuleUtil.getHostnameFromRule(resourceRule))
              .setPort(resourceRuleUtil.getPortFromRule(resourceRule)).build());
        }
      }
      sdcKeysManager.storeSecretKeys(resourceKeyList);
    } catch (IOException e) {
      throw new ResourceException(e);
    }
  }
}
