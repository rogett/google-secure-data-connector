/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

  /** The fake properties file we generate the config from */
  private List<ResourceRule> configResourceRules;
  private List<ResourceRule> runtimeResourceRules;
  private ResourceRule runtimeHttpResourceRule;
  private ResourceRule runtimeSocketResourceRule;
  private ResourceRule configHttpResourceRule;
  private ResourceRule configSocketResourceRule;

  /**
   * Creates a configuration beans from the fake hardcoded XML files.  
   */
  public FakeResourceRuleConfig() {

    // Configure each resource rule to match XML.
    runtimeHttpResourceRule = getBaseResourceRule();
    runtimeHttpResourceRule.setPattern(HTTP_PATTERN);
    runtimeHttpResourceRule.setRuleNum(HTTP_RULE_NUM);
    runtimeHttpResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeHttpResourceRule.setHttpProxyPort(Integer.valueOf(HTTP_PROXY_PORT));
    runtimeHttpResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    runtimeHttpResourceRule.setAppIds(APPID);
    runtimeSocketResourceRule = getBaseResourceRule();
    runtimeSocketResourceRule.setPattern(SOCKET_PATTERN);
    runtimeSocketResourceRule.setRuleNum(SOCKET_RULE_NUM);
    runtimeSocketResourceRule.setSecretKey(Long.valueOf(SECRET_KEY));
    runtimeSocketResourceRule.setSocksServerPort(Integer.valueOf(SOCKS_SERVER_PORT));
    configHttpResourceRule = getBaseResourceRule();
    configHttpResourceRule.setPattern(HTTP_PATTERN);
    configHttpResourceRule.setRuleNum(HTTP_RULE_NUM);
    configHttpResourceRule.setAppIds(APPID);
    configSocketResourceRule = getBaseResourceRule();
    configSocketResourceRule.setPattern(SOCKET_PATTERN);
    configSocketResourceRule.setRuleNum(SOCKET_RULE_NUM);
    
    // Add to Lists
    configResourceRules = new ArrayList<ResourceRule>();
    configResourceRules.add(configHttpResourceRule);
    configResourceRules.add(configSocketResourceRule);
    runtimeResourceRules = new ArrayList<ResourceRule>();
    runtimeResourceRules.add(runtimeHttpResourceRule);
    runtimeResourceRules.add(runtimeSocketResourceRule);
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
  
  private ResourceRule getBaseResourceRule() {
    ResourceRule resourceRule = new ResourceRule();
    resourceRule.setClientId(CLIENT_ID);
    resourceRule.setAllowedEntities(ALLOWED_ENTITY);
    return resourceRule;
  }
  

  public static final String CONFIG_RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "<ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "<allowedEntities>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "<appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "<appIds>" + APPID[1] + "</appIds>\n" +
    "<pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "<ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "<allowedEntities>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "<pattern>" + SOCKET_PATTERN + "</pattern>\n" +
    "</entity>\n" +
    "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "<ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "<allowedEntities>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "<appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "<appIds>" + APPID[1] + "</appIds>\n" +
    "<pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "<httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "<socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "<secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "<ruleNum>" + SOCKET_RULE_NUM + "</ruleNum>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "<allowedEntities>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "<pattern>" + SOCKET_PATTERN + "</pattern>\n" +
    "<socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "<secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n" +
    "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_ENTITY_XML =
    "<entity repeatable='true'>\n" +
    "<ruleNum>" + HTTP_RULE_NUM + "</ruleNum>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<allowedEntities repeatable='true'>" + ALLOWED_ENTITY[0] + "</allowedEntities>\n" +
    "<allowedEntities>" + ALLOWED_ENTITY[1] + "</allowedEntities>\n" +
    "<appIds repeatable='true'>" + APPID[0] + "</appIds>\n" +
    "<appIds>" + APPID[1] + "</appIds>\n" +
    "<pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "<httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "<socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "<secretKey>" + SECRET_KEY +"</secretKey>\n" +
    "</entity>\n";
}
