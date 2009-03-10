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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeResourceRuleConfig {

  // Resource Values
  public static final String CLIENT_ID = "all";
  public static final int HTTP_RULE_NUM = 1;
  public static final String[] ALLOWED_ENTITY = 
      { "rcolline@test.joonix.net", "admin@test.joonix.net" };
  public static final String[] APPID = { "someappid", "someappid2" };
  public static final String HTTP_PATTERN = "http://www.example.com";
  public static final int SOCKET_RULE_NUM = 2;
  public static final String SOCKET_PATTERN = "socket://128.195.131";
  public static final String HTTP_PROXY_PORT = "10000";
  public static final String SOCKS_SERVER_PORT = "1080";
  public static final String SECRET_KEY = "23423432432";
  public static final int URL_EXACT_RULE_NUM = 3;
  public static final String URL_EXACT_PATTERN = "http://www.example.com/exact/path";
  public static final int HTTPS_RULE_NUM = 4;
  public static final String HTTPS_PATTERN = "https://www.example.com";

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
    runtimeHttpResourceRule.setHttpProxyPort(Integer.valueOf(HTTP_PROXY_PORT));
    runtimeHttpResourceRule.setAppIds(APPID);
    
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
    runtimeUrlExactResourceRule.setHttpProxyPort(Integer.valueOf(HTTP_PROXY_PORT));
    
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
    configHttpResourceRule.setAppIds(APPID);
    configHttpResourceRule.setAppIds(APPID);
    
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
    resourceRule.setClientId(CLIENT_ID);
    resourceRule.setAllowedEntities(ALLOWED_ENTITY);
    resourceRule.setAppIds(APPID);
    return resourceRule;
  }
  
  public static final String CONFIG_RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + SOCKET_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + URL_EXACT_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + URL_EXACT_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.URLEXACT + "</patternType>\n" +
    "  </entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + HTTPS_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + HTTPS_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "</entity>\n" +
    "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + SOCKET_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + URL_EXACT_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + URL_EXACT_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.URLEXACT + "</patternType>\n" +
    "  <httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "  <ruleNum>" + HTTPS_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + HTTPS_PATTERN + "</pattern>\n" +
    "  <patternType>" + ResourceRule.HOSTPORT + "</patternType>\n" +
    "  <httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_ENTITY_XML =
    "<entity repeatable='true'>\n" +
    "  <ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "  <clientId>" + CLIENT_ID + "</clientId>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "  <allowedEntities repeatable='true'>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "  <appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "  <appIds repeatable='true'>" + APPID[1] + "</appIds>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "  <socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "  <secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n";
}
