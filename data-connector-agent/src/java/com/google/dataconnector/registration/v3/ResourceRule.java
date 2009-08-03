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

package com.google.dataconnector.registration.v3;

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
  
  @Deprecated
  public static final String REGEX = "REGEX";
  
  private int ruleNum;
  private String agentId;
  private String[] viewerEmail;
  private boolean allowDomainViewers;
  private AppTag[] apps;
  private String url;
  private String urlMatch;
  private Integer httpProxyPort;
  private Integer socksServerPort;
  private Long secretKey;

  public int getRuleNum() {
    return ruleNum;
  }

  public void setRuleNum(int ruleNum) {
    this.ruleNum = ruleNum;
  }

  @Deprecated
  public String getClientId() {
    return agentId;
  }

  @Deprecated
  public void setClientId(String clientId) {
    this.agentId = clientId;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  @Deprecated
  public String[] getAllowedEntities() {
    return viewerEmail;
  }

  @Deprecated
  public void setAllowedEntities(String[] allowedEntities) {
    this.viewerEmail = allowedEntities;
  }

  public String[] getViewerEmail() {
    return viewerEmail;
  }

  public void setViewerEmail(String[] viewerEmail) {
    this.viewerEmail = viewerEmail;
  }

  public boolean getAllowDomainViewers() {
    return allowDomainViewers;
  }

  public void setAllowDomainViewers(boolean allowDomainViewers) {
    this.allowDomainViewers = allowDomainViewers;
  }
  
  public AppTag[] getApps() {
    return apps;
  }

  public void setApps(AppTag[] apps) {
    this.apps = apps;
  }
  
  @Deprecated
  public String getPattern() {
    return url;
  }

  @Deprecated
  public void setPattern(String pattern) {
    this.url = pattern;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Deprecated
  public String getPatternType() {
    return urlMatch;
  }
  
  @Deprecated
  public void setPatternType(String patternType) {
    this.urlMatch = patternType;
  }

  public String getUrlMatch() {
    return urlMatch;
  }

  public void setUrlMatch(String urlMatch) {
    this.urlMatch = urlMatch;
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
    @Deprecated
    private String container;
    private String service;
    private String appId;
    private boolean allowAnyAppId;
    private boolean allowAnyPrivateGadget;
    
    @Deprecated
    public String getContainer() {
      return service;
    }
    
    @Deprecated
    public void setContainer(String container) {
      this.service = container;
    }
    
    public String getService() {
      return service;
    }

    public void setService(String service) {
      this.service = service;
    }

    public String getAppId() {
      return appId;
    }
    
    public void setAppId(String appId) {
      this.appId = appId;
    }
    
    public boolean getAllowAnyAppId() {
      return allowAnyAppId;
    }
    
    public void setAllowAnyAppId(boolean allowAnyAppId) {
      this.allowAnyAppId = allowAnyAppId;
    }
    
    public boolean isAllowAnyPrivateGadget() {
      return allowAnyPrivateGadget;
    }

    public void setAllowAnyPrivateGadget(boolean allowAnyPrivateGadget) {
      this.allowAnyPrivateGadget = allowAnyPrivateGadget;
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
