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

package com.google.dataconnector.util;

import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.feedserver.util.ConfigFile;
import com.google.feedserver.util.Flag;

/**
 * Bean representing all the startup configuration for the agent.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class LocalConf {
  
  private static final String DEFAULT_BIND_HOST = "127.0.0.1";
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
  
  // for authn, either oauthkey or password should be present.
  @Flag(help = "Two-legged oauth consumer key.")
  private String oauthKey;
  @Flag(help = "Password.")
  private String password;
  
  @Flag(help = "Keystore password if using external keystore file")
  private String sslKeyStorePassword = DEFAULT_SSL_KEYSTORE_PASSWORD;
  @Flag(help = "External keystore to use.  Default only allows verified certs per java default " +
      "trust store.")
  private String sslKeyStoreFile;
  @Flag(help = "Client identifier for this agent.  These must be unique for all agents in this " +
      "domain.")
  private String clientId;
  @Flag(help = "Location of sshd binary to use for SDC protocol multiplexor")
  private String sshd;
  @Flag(help = "Starting http proxy port to assign for each HTTP Resource Rule")
  private Integer httpProxyPort;
  @Flag(help = "Default bind host is localhost, One should not have to change this.")
  private String httpProxyBindHost = DEFAULT_BIND_HOST;
  @Flag(help = "Port to bind socks firewall port to.")
  private Integer socksServerPort;
  @Flag(help = "System apache2 htpassword location")
  private String apacheHtpasswd = "third-party/apache-httpd/root/bin/htpassword";
  @Flag(help = "System apache2 apachectl location")
  private String apacheCtl = "third-party/apache-httpd/root/bin/apachectl";
  @Flag(help = "Apache configuration files.  Must be writable by user agent runs as.")
  private String apacheConfDir;
  @Flag(help = "Turn on debug logging.")
  private Boolean debug = DEBUG;
  
  // Config File Only
  private String logProperties;
  private String socksProperties = 
      "iddleTimeout = 60000\n" + // 10 minutes
      "acceptTimeout = 60000\n" + // 1 minutes
      "udpTimeout = 600000\n" + // 10 minutes
      "log = -\n"; // stdout.
  
  private AuthRequest.AuthType authType = AuthRequest.AuthType.NONE;
  
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

  public String getOauthKey() {
    return oauthKey;
  }

  public void setOauthKey(String oauthKey) {
    this.oauthKey = oauthKey;
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

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getSshd() {
    return sshd;
  }

  public void setSshd(String sshd) {
    this.sshd = sshd;
  }

  public Integer getHttpProxyPort() {
    return httpProxyPort;
  }

  public void setHttpProxyPort(Integer httpProxyPort) {
    this.httpProxyPort = httpProxyPort;
  }

  public String getHttpProxyBindHost() {
    return httpProxyBindHost;
  }

  public void setHttpProxyBindHost(String httpProxyBindHost) {
    this.httpProxyBindHost = httpProxyBindHost;
  }

  public Integer getSocksServerPort() {
    return socksServerPort;
  }

  public void setSocksServerPort(Integer socksServerPort) {
    this.socksServerPort = socksServerPort;
  }

  public String getLogProperties() {
    return logProperties;
  }

  public void setLogProperties(String logProperties) {
    this.logProperties = logProperties;
  }

  public String getSocksProperties() {
    return socksProperties;
  }

  public void setSocksProperties(String socksProperties) {
    this.socksProperties = socksProperties;
  }
  
  public String getApacheHtpasswd() {
    return apacheHtpasswd;
  }

  public void setApacheHtpasswd(String apacheHtpasswd) {
    this.apacheHtpasswd = apacheHtpasswd;
  }

  public String getApacheCtl() {
    return apacheCtl;
  }

  public void setApacheCtl(String apacheCtl) {
    this.apacheCtl = apacheCtl;
  }

  public String getApacheConfDir() {
    return apacheConfDir;
  }

  public void setApacheConfDir(String apacheConfDir) {
    this.apacheConfDir = apacheConfDir;
  }
  
  public AuthRequest.AuthType getAuthType() {
    return authType;
  }

  public void setAuthType(AuthRequest.AuthType authType) {
    this.authType = authType;
  }

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }
}
