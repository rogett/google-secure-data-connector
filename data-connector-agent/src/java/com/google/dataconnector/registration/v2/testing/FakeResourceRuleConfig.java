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
package com.google.dataconnector.registration.v2.testing;

import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRule.AppTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeResourceRuleConfig {

  // Resource Values
  public static final String AGENT_ID = "all";
  public static final int HTTP_RULE_NUM = 1;
  public static final String[] ALLOWED_ENTITY = 
      { "rcolline@test.joonix.net", "admin@test.joonix.net" };
  public static final AppTag[] APPID;
  public static final String HTTP_PATTERN = "http://www.example.com";
  public static final int SOCKET_RULE_NUM = 2;
  public static final String SOCKET_PATTERN = "socket://128.195.131";
  public static final String SOCKS_SERVER_PORT = "1080";
  public static final String SECRET_KEY = "23423432432";
  public static final int URL_EXACT_RULE_NUM = 3;
  public static final String URL_EXACT_PATTERN = "http://www.example.com/exact/path";
  public static final int HTTPS_RULE_NUM = 4;
  public static final String HTTPS_PATTERN = "https://www.example.com";

  static {
    APPID = new AppTag[2];
    APPID[0] = new AppTag();
    APPID[0].setContainer("somecontainer");
    APPID[0].setAppId("someappid");
    APPID[1] = new AppTag();
    APPID[1].setContainer("somecontainer2");
    APPID[1].setAppId("someappid2");
  }
  
  /** The fake properties file we generate the config from */
  private List<ResourceRule> configResourceRules;
  private List<ResourceRule> runtimeResourceRules;
  private ResourceRule runtimeHttpResourceRule;
  private ResourceRule runtimeSocketResourceRule;
  private ResourceRule runtimeUrlExactResourceRule;
  private ResourceRule runtimeHttpsResourceRule;
  private ResourceRule configHttpResourceRule;
  private ResourceRule configSocketResourceRule;
  private ResourceRule configUrlExactResourceRule;
  private ResourceRule configHttpsResourceRule;

  /**
   * Creates a configuration beans from the fake hardcoded XML files.  
   */
  public FakeResourceRuleConfig() {

    // Configure each resource rule to match XML.
    runtimeHttpResourceRule = getBaseResourceRule();
    runtimeHttpResourceRule.setPattern(HTTP_PATTERN);
    runtimeHttpResourceRule.setPatternType(ResourceRule.HOSTPORT);
    runtimeHttpResourceRule.setRuleNum(HTTP_RULE_NUM);
    runtimeHttpResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeHttpResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    runtimeHttpResourceRule.setApps(APPID);
    
    runtimeSocketResourceRule = getBaseResourceRule();
    runtimeSocketResourceRule.setPattern(SOCKET_PATTERN);
    runtimeSocketResourceRule.setPatternType(ResourceRule.HOSTPORT);
    runtimeSocketResourceRule.setRuleNum(SOCKET_RULE_NUM);
    runtimeSocketResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeSocketResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    
    runtimeUrlExactResourceRule = getBaseResourceRule();
    runtimeUrlExactResourceRule.setPattern(URL_EXACT_PATTERN);
    runtimeUrlExactResourceRule.setPatternType(ResourceRule.URLEXACT);
    runtimeUrlExactResourceRule.setRuleNum(URL_EXACT_RULE_NUM);
    runtimeUrlExactResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeUrlExactResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    
    runtimeHttpsResourceRule = getBaseResourceRule();
    runtimeHttpsResourceRule.setPattern(HTTPS_PATTERN);
    runtimeHttpsResourceRule.setPatternType(ResourceRule.HOSTPORT);
    runtimeHttpsResourceRule.setRuleNum(HTTPS_RULE_NUM);
    runtimeHttpsResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeHttpsResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    
    configHttpResourceRule = getBaseResourceRule();
    configHttpResourceRule = getBaseResourceRule();
    configHttpResourceRule.setPattern(HTTP_PATTERN);
    configHttpResourceRule.setPatternType(ResourceRule.HOSTPORT);
    configHttpResourceRule.setRuleNum(HTTP_RULE_NUM);
    configHttpResourceRule.setApps(APPID);
    
    configSocketResourceRule = getBaseResourceRule();
    configSocketResourceRule.setPattern(SOCKET_PATTERN);
    configSocketResourceRule.setPatternType(ResourceRule.HOSTPORT);
    configSocketResourceRule.setRuleNum(SOCKET_RULE_NUM);
    
    configUrlExactResourceRule = getBaseResourceRule();
    configUrlExactResourceRule.setPattern(URL_EXACT_PATTERN);
    configUrlExactResourceRule.setPatternType(ResourceRule.URLEXACT);
    configUrlExactResourceRule.setRuleNum(URL_EXACT_RULE_NUM);
    
    configHttpsResourceRule = getBaseResourceRule();
    configHttpsResourceRule.setPattern(HTTPS_PATTERN);
    configHttpsResourceRule.setPatternType(ResourceRule.HOSTPORT);
    configHttpsResourceRule.setRuleNum(HTTPS_RULE_NUM);
    
    // Add to Lists
    configResourceRules = new ArrayList<ResourceRule>();
    configResourceRules.add(configHttpResourceRule);
    configResourceRules.add(configSocketResourceRule);
    configResourceRules.add(configUrlExactResourceRule);
    configResourceRules.add(configHttpsResourceRule);
    runtimeResourceRules = new ArrayList<ResourceRule>();
    runtimeResourceRules.add(runtimeHttpResourceRule);
    runtimeResourceRules.add(runtimeSocketResourceRule);
    runtimeResourceRules.add(runtimeUrlExactResourceRule);
    runtimeResourceRules.add(runtimeHttpsResourceRule);
  }

  /**
   * @return list of ResourceRule populated with a fake set of configuration from 
   * {@link #CONFIG_RESOURCE_RULES_XML}
   */
  public List<ResourceRule> getFakeConfigResourceRules() {
    return configResourceRules;
  }
  
  /**
   * @return list of ResourceRule populated with a fake set of configuration from 
   * {@link #CONFIG_RESOURCE_RULES_XML}
   */
  public List<ResourceRule> getFakeRuntimeResourceRules() {
    return runtimeResourceRules;
  }
  
  public ResourceRule getRuntimeHttpResourceRule() {
    return runtimeHttpResourceRule;
  }

  public ResourceRule getRuntimeSocketResourceRule() {
    return runtimeSocketResourceRule;
  }

  public ResourceRule getConfigHttpResourceRule() {
    return configHttpResourceRule;
  }

  public ResourceRule getConfigSocketResourceRule() {
    return configSocketResourceRule;
  }
  
  public ResourceRule getConfigUrlExactResourceRule() {
    return configUrlExactResourceRule;
  }

  public ResourceRule getRuntimeUrlExactResourceRule() {
    return runtimeUrlExactResourceRule;
  }
  
  private ResourceRule getBaseResourceRule() {
    ResourceRule resourceRule = new ResourceRule();
    resourceRule.setClientId(AGENT_ID);
    resourceRule.setAllowedEntities(ALLOWED_ENTITY);
    resourceRule.setApps(APPID);
    return resourceRule;
  }
  
  public static final String CONFIG_RESOURCE_RULES_XML = "<resourceRules>\n" +
    "<rule repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[0] + "</viewerEmail>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[1] + "</viewerEmail>\n" +
    "  <apps repeatable='true'><service>" + APPID[0].getContainer() + "</service><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><service>" + APPID[1].getContainer() + "</service><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <url>" + HTTP_PATTERN + "</url>\n" +
    "  <urlMatch>" + ResourceRule.HOSTPORT + "</urlMatch>\n" +
    "</rule>\n" +
    "<rule>\n" +
    "  <ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[0] + "</viewerEmail>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[1] + "</viewerEmail>\n" +
    "  <apps repeatable='true'><service>" + APPID[0].getContainer() + "</service><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><service>" + APPID[1].getContainer() + "</service><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <url>" + SOCKET_PATTERN + "</url>\n" +
    "  <urlMatch>" + ResourceRule.HOSTPORT + "</urlMatch>\n" +
    "</rule>\n" +
    "<rule>\n" +
    "  <ruleNum>" + URL_EXACT_RULE_NUM + "</ruleNum>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[0] + "</viewerEmail>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[1] + "</viewerEmail>\n" +
    "  <apps repeatable='true'><service>" + APPID[0].getContainer() + "</service><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><service>" + APPID[1].getContainer() + "</service><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <url>" + URL_EXACT_PATTERN + "</url>\n" +
    "  <urlMatch>" + ResourceRule.URLEXACT + "</urlMatch>\n" +
    "</rule>\n" +
    "<rule>\n" +
    "  <ruleNum>" + HTTPS_RULE_NUM + "</ruleNum>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[0] + "</viewerEmail>\n" +
    "  <viewerEmail repeatable='true'>" + ALLOWED_ENTITY[1] + "</viewerEmail>\n" +
    "  <apps repeatable='true'><service>" + APPID[0].getContainer() + "</service><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><service>" + APPID[1].getContainer() + "</service><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <url>" + HTTPS_PATTERN + "</url>\n" +
    "  <urlMatch>" + ResourceRule.HOSTPORT + "</urlMatch>\n" +
    "</rule>\n" +
    "</resourceRules>\n" ;
  
  public static final String RUNTIME_RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <apps repeatable='true'><container>" + APPID[0].getContainer() + "</container><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><container>" + APPID[1].getContainer() + "</container><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <apps repeatable='true'><container>" + APPID[0].getContainer() + "</container><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><container>" + APPID[1].getContainer() + "</container><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <pattern>" + SOCKET_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + URL_EXACT_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <apps repeatable='true'><container>" + APPID[0].getContainer() + "</container><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><container>" + APPID[1].getContainer() + "</container><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <pattern>" + URL_EXACT_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.URLEXACT + "</patternType>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + HTTPS_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <apps repeatable='true'><container>" + APPID[0].getContainer() + "</container><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><container>" + APPID[1].getContainer() + "</container><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <pattern>" + HTTPS_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_ENTITY_XML =
    "<entity repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <apps repeatable='true'><container>" + APPID[0].getContainer() + "</container><appId>" + APPID[0].getAppId() + "</appId></apps>\n" +
    "  <apps repeatable='true'><container>" + APPID[1].getContainer() + "</container><appId>" + APPID[1].getAppId() + "</appId></apps>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n";
}
