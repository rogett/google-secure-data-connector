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

package com.google.dataconnector.registration.v2;

/**
 * Bean representing all the Secure Data Connector Agent configuration.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRule implements Comparable<ResourceRule> {
  
  // URL resource type identifiers
  public static final String HTTPID = "http://";
  public static final String HTTPSID = "https://";
  public static final String SOCKETID = "socket://";
  
  public static final String HOSTPORT = "HOSTPORT";
  public static final String URLEXACT = "URLEXACT";
  public static final String REGEX = "REGEX";
  
  private int ruleNum;
  // this exists for backward compatibility. clients with ruleNum field will not have this field
  private String name; 
  
  private String clientId;
  private String[] allowedEntities;
  private AppTag[] apps;
  private String pattern;
  private String patternType;
  private Integer httpProxyPort;
  private Integer socksServerPort;
  private Long secretKey;
  
  // getters and setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getRuleNum() {
    return ruleNum;
  }

  public void setRuleNum(int ruleNum) {
    this.ruleNum = ruleNum;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String[] getAllowedEntities() {
    return allowedEntities;
  }

  public void setAllowedEntities(String[] allowedEntities) {
    this.allowedEntities = allowedEntities;
  }

  public AppTag[] getApps() {
    return apps;
  }

  public void setApps(AppTag[] apps) {
    this.apps = apps;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getPatternType() {
    return patternType;
  }
  
  public void setPatternType(String patternType) {
    this.patternType = patternType;
  }

  public Integer getHttpProxyPort() {
    return httpProxyPort;
  }

  public void setHttpProxyPort(Integer httpProxyPort) {
    this.httpProxyPort = httpProxyPort;
  }

  public void setSocksServerPort(Integer socksServerPort) {
    this.socksServerPort = socksServerPort;
  }

  public Integer getSocksServerPort() {
    return socksServerPort;
  }

  public Long getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(Long secretKey) {
    this.secretKey = secretKey;
  }

  /**
   * Compares two rules by sequence number.
   */
  public int compareTo(ResourceRule o) {
    if (getRuleNum() < o.getRuleNum()) {
      return -1;
    } else if (getRuleNum() > o.getRuleNum()) {
      return 1;
    } else {
      return 0;
    }
  }
  

  /**
   * Models an App identification field. 
   */
  public static class AppTag implements Comparable<AppTag>{
    private String container;
    private String appId;
    
    public void setContainer(String container) {
      this.container = container;
    }
    public void setAppId(String appId) {
      this.appId = appId;
    }
    public String getContainer() {
      return container;
    }
    public String getAppId() {
      return appId;
    }
    
    @Override
    public int compareTo(AppTag o) {
      int compare = container.compareTo(o.container);
      if (compare != 0) {
        return compare;
      }
      return appId.compareTo(o.appId);
    }
  }
}
