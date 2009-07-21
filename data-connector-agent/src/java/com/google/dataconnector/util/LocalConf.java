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
package com.google.dataconnector.util;

import com.google.feedserver.util.ConfigFile;
import com.google.feedserver.util.Flag;
import com.google.inject.Singleton;

/**
 * Bean representing all the startup configuration for the agent.
 * 
 * @author rayc@google.com (Ray Colline)
 */
@Singleton
public class LocalConf {
  
  private static final String DEFAULT_GOOGLE_SDC_HOST = "apps-secure-data-connector.google.com";
  private static final int DEFAULT_GOOGLE_SDC_PORT = 443;
  private static final String DEFAULT_SSL_KEYSTORE_PASSWORD = "woodstock";
  private static boolean DEBUG = false;
  public static final String HTTPD_CONF_TEMPLATE_FILE = "httpd.conf-template";
  public static final String DEFAULT_SOCKS_BIND_HOST = "127.0.0.1";
  
  private String name;

  @ConfigFile(required = true)
  @Flag(help = "Configuration File")
  private String localConfigFile = ""; //Configure flag to be of type "String"
  @Flag(help = "Agent firewall rules configuration file.")
  private String rulesFile;
  @Flag(help = "SDC server host to connect to.")
  private String sdcServerHost = DEFAULT_GOOGLE_SDC_HOST;
  @Flag(help = "SDC server port to connect to.")
  private Integer sdcServerPort = DEFAULT_GOOGLE_SDC_PORT;
  @Flag(help = "Google Apps domain to associate agent with.")
  private String domain;
  
  @Flag(help = "Any valid admin user on the domain. This is only used for authentication.")
  private String user;
  @Flag(help = "Password.")
  private String password;
  
  @Flag(help = "Keystore password if using external keystore file")
  private String sslKeyStorePassword = DEFAULT_SSL_KEYSTORE_PASSWORD;
  @Flag(help = "External keystore to use.  Default only allows verified certs per java default " +
      "trust store.")
  private String sslKeyStoreFile;
  @Flag(help = "Agent identifier for this agent.  These must be unique for all agents in this " +
      "domain.")
  private String agentId;
  @Flag(help = "Location of sshd binary to use for SDC protocol multiplexor")
  private String sshd;
  @Flag(help = "Port to bind socks firewall port to.")
  private Integer socksServerPort;
  @Flag(help = "Turn on debug logging.")
  private Boolean debug = DEBUG;
  @Flag(help = "Allow unverified certificates")
  private Boolean allowUnverifiedCertificates = false;
  @Flag(help = "the users who can access the healthcheck gadget")
  private String healthCheckGadgetUsers;
  @Flag(help = "log4j properties File")
  private String log4jPropertiesFile;
  
  // Config File Only
  private String socksProperties = 
      "iddleTimeout = 60000\n" + // 10 minutes
      "acceptTimeout = 60000\n" + // 1 minutes
      "udpTimeout = 600000\n" + // 10 minutes
      "log = -\n"; // stdout.
  
  // getters and setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocalConfigFile() {
    return localConfigFile;
  }

  public void setLocalConfigFile(String configFile) {
    this.localConfigFile = configFile;
  }
  
  public void setRulesFile(String rulesFile) {
    this.rulesFile = rulesFile;
  }

  public String getRulesFile() {
    return rulesFile;
  }

  public String getSdcServerHost() {
    return sdcServerHost;
  }

  public void setSdcServerHost(String sdcServerHost) {
    this.sdcServerHost = sdcServerHost;
  }

  public Integer getSdcServerPort() {
    return sdcServerPort;
  }

  public void setSdcServerPort(Integer sdcServerPort) {
    this.sdcServerPort = sdcServerPort;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getSslKeyStorePassword() {
    return sslKeyStorePassword;
  }

  public void setSslKeyStorePassword(String sslKeyStorePassword) {
    this.sslKeyStorePassword = sslKeyStorePassword;
  }

  public String getSslKeyStoreFile() {
    return sslKeyStoreFile;
  }

  public void setSslKeyStoreFile(String sslKeyStoreFile) {
    this.sslKeyStoreFile = sslKeyStoreFile;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String clientId) {
    this.agentId = clientId;
  }

  public String getSshd() {
    return sshd;
  }

  public void setSshd(String sshd) {
    this.sshd = sshd;
  }

  public Integer getSocksServerPort() {
    return socksServerPort;
  }

  public void setSocksServerPort(Integer socksServerPort) {
    this.socksServerPort = socksServerPort;
  }

  public String getSocksProperties() {
    return socksProperties;
  }

  public void setSocksProperties(String socksProperties) {
    this.socksProperties = socksProperties;
  }
  
  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public void setAllowUnverifiedCertificates(Boolean allowUnverifiedCertificates) {
    this.allowUnverifiedCertificates = allowUnverifiedCertificates;
  }

  public Boolean getAllowUnverifiedCertificates() {
    return allowUnverifiedCertificates;
  }
    
  public String getHealthCheckGadgetUsers() {
    return healthCheckGadgetUsers;
  }

  public void setHealthCheckGadgetUsers(String healthCheckGadgetUsers) {
    this.healthCheckGadgetUsers = healthCheckGadgetUsers;
  }

  public String getLog4jPropertiesFile() {
    return log4jPropertiesFile;
  }

  public void setLog4jPropertiesFile(String log4jPropertiesFile) {
    this.log4jPropertiesFile = log4jPropertiesFile;
  }
}
