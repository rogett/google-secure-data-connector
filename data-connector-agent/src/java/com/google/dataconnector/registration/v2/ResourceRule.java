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
  
  private int ruleNum;
  // this exists for backward compatibility. clients with ruleNum field will not have this field
  private String name; 
  
  private String clientId;
  private String[] allowedEntities;
  private String[] appIds;
  private String pattern;
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

  public String[] getAppIds() {
    return appIds;
  }

  public void setAppIds(String[] appIds) {
    this.appIds = appIds;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
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
}
