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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The Secure Link Client global configuration object.  All configuration should be
 * added here going forward.  Flags are automatically added for every static final
 * String field ending in "_KEY".  
 * 
 * The ClientConf is populated from a properties instance(loaded from the file specified by 
 * the --config_file flag and we provide a public accessor {@link #getConfigFileFlagValue()}
 * to boot strap the configuration.
 * 
 * All properties except resources can be overridden by a command line flag of the same name.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientConf {
  
  // Default bind host (dont expose these services to the outside)
  private static final String DEFAULT_BIND_HOST = "127.0.0.1";

  // Configuration Parameters
  static final String CONFIG_FILE_KEY = "configFile"; // Flag definition
  private String configFile = ""; //Configure flag to be of type "String"
  
  private static final String SECURE_LINK_SERVER_HOST_KEY = "secureLinkServerHost";
  private String secureLinkServerHost;
  
  private static final String SECURE_LINK_SERVER_PORT_KEY = "secureLinkServerPort";
  private Integer secureLinkServerPort;

  private static final String USE_SSL_KEY = "useSsl";
  private Boolean useSsl;
  
  private static final String SSL_KEY_STORE_PASSWORD_KEY = "sslKeyStorePassword";
  private String sslKeyStorePassword = "";
  
  private static final String SSL_KEY_STORE_FILE_KEY = "sslKeyStoreFile";
  private String sslKeyStoreFile = "";
  
  // Google Apps Hosted Domain owner for this Secure Link connection.
  private static final String DOMAIN_KEY = "domain";
  private String domain;

  // Authorized Google Apps Hosted Admin User
  private static final String USER_KEY = "user";
  private String user;

  // Password transmitted over SSL.
  private static final String PASSWORD_KEY = "password";
  private String password;

  // Unique ID identifying this client to the Secure Link Server.
  private static final String CLIENT_ID_KEY = "clientId";
  private String clientId;

  // Location of sshd execution script.
  private static final String SSHD_KEY = "sshd";
  private String sshd;

  // First free port for WPG http proxy server to use.
  private static final String STARTING_HTTP_PROXY_PORT_KEY = "startingHttpProxyPort";
  private Integer startingHttpProxyPort;

  // Local bind address for SOCKS server
  private static final String HTTP_PROXY_BIND_HOST = "httpProxyBindHost";
  private String httpProxyBindHost;
  
  // Local listen port for SOCKS daemon.
  private static final String SOCKS_SERVER_PORT_KEY = "socksServerPort";
  private Integer socksServerPort;
  
  // Local bind address for SOCKS server
  private static final String SOCKSD_BIND_HOST = "socksdBindHost";
  private String socksdBindHost;
  
  // Logging configuration file
  private static final String LOG_PROPERTIES_FILE_KEY = "logPropertiesFile";
  private String logPropertiesFile;
  
  // The list of resources this client is sharing.
  private List<ResourceConfigEntry> rules;

  // Our Flags instance.
  private static CommandLine flags;
  
  // The raw global client properties this ClientConf is derived from.
  private Properties clientProps;
  
  /**
   * Populates the ClientConf instance with values from either the supplied {@link Properties}
   * instance or flags on the commandline.  
   * 
   * @param clientProps The Secure Link properties instance.
   * @throws ConfigurationException if any required configuration entries are missing or malformed.
   */
  public ClientConf(final Properties clientProps) throws ConfigurationException {
    this.clientProps = clientProps;
    domain = getAndCheckProperty(clientProps, DOMAIN_KEY);
    user = getAndCheckProperty(clientProps, USER_KEY);
    password = getAndCheckProperty(clientProps, PASSWORD_KEY);
    clientId = getAndCheckProperty(clientProps, CLIENT_ID_KEY);
    secureLinkServerHost = getAndCheckProperty(clientProps, SECURE_LINK_SERVER_HOST_KEY);
    secureLinkServerPort = getAndCheckIntegerProperty(clientProps, SECURE_LINK_SERVER_PORT_KEY);
    useSsl = getAndCheckBooleanProperty(clientProps, USE_SSL_KEY, true);
    sshd = getAndCheckProperty(clientProps, SSHD_KEY);
    startingHttpProxyPort = getAndCheckIntegerProperty(clientProps, STARTING_HTTP_PROXY_PORT_KEY);
    httpProxyBindHost = getAndCheckProperty(clientProps, HTTP_PROXY_BIND_HOST, DEFAULT_BIND_HOST);
    socksServerPort = getAndCheckIntegerProperty(clientProps, SOCKS_SERVER_PORT_KEY);
    socksdBindHost = getAndCheckProperty(clientProps, SOCKSD_BIND_HOST, DEFAULT_BIND_HOST);
    logPropertiesFile = getAndCheckProperty(clientProps, LOG_PROPERTIES_FILE_KEY, 
        "logging.properties");
    sslKeyStorePassword = getAndCheckProperty(clientProps, SSL_KEY_STORE_PASSWORD_KEY, "");
    sslKeyStoreFile = getAndCheckProperty(clientProps, SSL_KEY_STORE_FILE_KEY, 
        "config/secureLinkClientTrustStore"); 
    rules = processResourceEntries(clientProps, startingHttpProxyPort);
  }
  
  /**
   * Utility method that reads all resource entries in the properties and creates their
   * associated {@link ResourceConfigEntry} object.  
   * 
   * @param clientProps The Secure Link Client configuration properties file.
   * @returns a list of populated ResourceConfigEntry objects. 
   * @throws ConfigurationException if any invalid resources entries are encountered.
   */
   static List<ResourceConfigEntry> processResourceEntries(final Properties clientProps,
      int startingHttpProxyPort) throws ConfigurationException {
    // Loop through all rules and create the appropriate type of resource entry.
    List<ResourceConfigEntry> rules = new ArrayList<ResourceConfigEntry>();
    int proxyPort = startingHttpProxyPort;
    for (String keyName : clientProps.stringPropertyNames()) {
      if (keyName.startsWith("resource")) {
        int resourceSeqNum;
        try {
          resourceSeqNum = Integer.parseInt(keyName.substring(8, keyName.length()));
        } catch (NumberFormatException e) {
          // invalid resource specification
          throw new ConfigurationException("Invalid firewall rule: '" + keyName +
              "'. Left side of the Resource rule should be 'resource<number>'" );
        }
        String value = clientProps.getProperty(keyName);
        try {
          String[] resourceStr = ClientConf.parseResourceRule(value);
          String allowedEntities = resourceStr[1];
          String pattern = resourceStr[0];          
          ResourceConfigEntry entry = null;
          if (pattern.startsWith(SocketResourceConfigEntry.URLID)) {
            entry = new SocketResourceConfigEntry(pattern, allowedEntities, resourceSeqNum);
          } else if (pattern.startsWith(UriResourceConfigEntry.HTTPURLID) || 
              pattern.startsWith(UriResourceConfigEntry.HTTPSURLID)) {
            entry = new UriResourceConfigEntry(pattern, allowedEntities, proxyPort, resourceSeqNum);
            proxyPort++; // We create a proxy server per URL pattern.
          }
          if (null != entry) {
            rules.add(entry);
          }
        } catch (ResourceConfigException e) {
          throw new ConfigurationException("Invalid firewall rule: '" + keyName + "', pattern: '" +
              value + "', reason for error: " + e.getMessage());
        }
      }
    }
    return rules;
  }
   
  /**
   * Retrieves the specified property from the given {@link Properties} instance and throws
   * an exception if its null.  This method is for required properties.
   * 
   * @param props the properties instance to get the value from.
   * @param keyName the key to fetch.
   * @return String representing the value of the property.
   * @throws ConfigurationException if the property is non existent.
   */
  static String getAndCheckProperty(final Properties props, final String keyName) 
      throws ConfigurationException {
    return getAndCheckProperty(props, keyName, null);
  }
  
  /**
   * Retrieves the specified property from the given {@link Properties} instance and throws
   * an exception if its null.  This version allows the setting of a defaultValue for 
   * properties that are not required.  
   * 
   * @param props the properties instance to get the value from.
   * @param keyName the key to fetch.
   * @param defaultValue value to set if not found.
   * @return String representing the value of the property.
   * @throws ConfigurationException if the property is non existent.
   */
  static String getAndCheckProperty(final Properties props, final String keyName, 
      final String defaultValue) throws ConfigurationException {
    String value = props.getProperty(keyName);
    value = flags.getOptionValue(keyName, value);
    if (value == null) {
      if (defaultValue == null) {
        throw new ConfigurationException("Missing configuration entry: " + keyName);
      }
      return defaultValue;
    }
    return value;
  }

  /**
   * Retrieves the specified property from the given {@link Properties} instance and looks
   * at the Command Line options.  The order of precedence is CLI, properties, then default
   * value.
   * 
   * @param props the properties instance to get the value from.
   * @param keyName the key to fetch.
   * @param defaultValue value to set if not found.
   * @return true if property has "true" set or if command line option is set.  False otherwise.
   */
  static Boolean getAndCheckBooleanProperty(final Properties props, final String keyName, 
      final Boolean defaultValue) {
    // Explicit negate trumps all.
    if (flags.hasOption("no" + keyName)) {
      return false;
    }
    String stringValue = props.getProperty(keyName);
    // Nothing was specified in the config file, use the default or the Command Line
    if (stringValue == null) {
      return flags.hasOption(keyName) || defaultValue;
    } else { // Something was specified, use it or the Command Line.
      return Boolean.valueOf(stringValue) || flags.hasOption(keyName);
    }
  }
  
  /**
   * Retrieves the specified Integer property from the given {@link Properties} instance and throws
   * an exception if its null.  This method is for required properties.
   * 
   * @param props the properties instance to get the value from.
   * @param keyName the key to fetch.
   * @return String representing the value of the property.
   * @throws ConfigurationException if the property is non existent or property is not a proper
   *             Integer.
   */
  static Integer getAndCheckIntegerProperty(final Properties props, final String keyName) 
      throws ConfigurationException {
    String stringIntValue = getAndCheckProperty(props, keyName);
    try {
      return new Integer(stringIntValue);
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Invalid numeric property: " + keyName + " value: " + 
          stringIntValue);
    }
  }

  /**
   * Using reflection this method registers an {@link Option} for every {@link Field} in 
   * the {@link ClientConf} class and then parses the flags from the supplied command line
   * argv array.  This static method will print usage and exit the VM if any flag parsing 
   * errors occur.  For the Secure Link Client, flags are used to override or 
   * fill in missing config file properties.  
   * 
   * @param args argv string from invocation.
   */
  public static void parseFlags(String [] args) {
    Options options = new Options();

    for (Field field: ClientConf.class.getDeclaredFields()) {
      if (field.getName().endsWith("_KEY")) {
        String fieldValue;
        try {
          fieldValue = (String) field.get(null);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        try {
          if (ClientConf.class.getDeclaredField((String) field.get(
              new String())).getType().getName().equals(Boolean.class.getName())) { // boolean flag
            options.addOption(new Option(fieldValue, false, fieldValue));
            options.addOption(new Option("no" + fieldValue, false, "negates " + fieldValue));
          } else {  // String flag.
            options.addOption(new Option(fieldValue, true, fieldValue));
          }
        } catch (NoSuchFieldException e) {
          throw new RuntimeException("Field " + field.getName() + " misconfigured: " + e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Field " + field.getName() + " misconfigured: " + e);
        }
      }
    }
    
    // Parse command line flags
    CommandLineParser parser = new GnuParser();
    try {
      flags = parser.parse(options, args);
    } catch (ParseException e) {
      new HelpFormatter().printHelp("Server", options);
      System.err.println("Usage error: " + e.getMessage());
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Arg error: " + e.getMessage());
      System.exit(1);
    }
  }
  
  public Properties getLoggingProperties() throws IOException {
    return loadPropertiesFile(logPropertiesFile);
  }
  
  /**
   * Returns the value for the config file flag.  Must call {@link #parseFlags(String[])} first.
   * 
   * @returns config file location specified on the command line or "client.Conf" if it hasnt been 
   *             set.
   */
  public static String getConfigFileFlagValue() {
    return flags.getOptionValue(CONFIG_FILE_KEY, "client.Conf");
  }
  
  /**
   * Convenience method that returns the user and domain in email address form by joining 
   * user@domain.
   * 
   * @return String representing the user and domain as an email address.
   */
  public String getEmail() {
    return user + "@" + domain;
  }

  /**
   * Because tests may need to mock the command line we allow them special access to the flags
   * instance.  One should never use this in production code!
   * 
   * @returns the flags instance used by the Client Configuration.
   */
  public static CommandLine getFlagsForTest() {
    return flags;
  }
  
  /**
   * Helper that loads a properties file from given filename. 
   * 
   * @param fileName properties file with rules configuration.
   * @returns a Properties object loaded with the rules configuration file.
   * @throws IOException if any errors are encountered reading the properties file
   */
  public static Properties loadPropertiesFile(final String fileName) throws IOException {
    InputStream fin = new FileInputStream(fileName);
    Properties props = new Properties();
    props.load(fin);
    fin.close();
    return props;
  }
  
  /**
   * Because tests may need to mock the command line we allow them special access to the flags
   * instance.  One should never use this in production code!
   * 
   * @param flags A mocked CommandLine instance.
   */
  public static void setFlagsForTest(CommandLine flags) {
    ClientConf.flags = flags;
  }
  
  /*
   * Below are getters and setters for all the config fields in this class.
   */
  
  /**
   * Adds a "PermitOpen" ACL on the commandline to only allow this server to talk to the
   * local socksd for port forwards.
   * 
   * @return the sshd command line with port forward ACL.
   */
  public String getSshd() {
    return sshd  + " -o PermitOpen=localhost:" + getSocksServerPort();
  }

  public void setSshd(String sshd) {
    this.sshd = sshd;
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

  public String getClient_id() {
    return clientId;
  }

  public void setClient_id(String client_id) {
    this.clientId = client_id;
  }

  public List<ResourceConfigEntry> getRules() {
    return rules;
  }

  public String getSecureLinkServerHost() {
    return secureLinkServerHost;
  }

  public void setSecureLinkServerHost(String secureLinkServerHost) {
    this.secureLinkServerHost = secureLinkServerHost;
  }

  public Integer getSecureLinkServerPort() {
    return secureLinkServerPort;
  }

  public void setSecureLinkServerPort(Integer secureLinkServerPort) {
    this.secureLinkServerPort = secureLinkServerPort;
  }

  public Integer getSocksServerPort() {
    return socksServerPort;
  }

  public void setSocksServerPort(Integer socksServerPort) {
    this.socksServerPort = socksServerPort;
  }

  public String getSocksdBindHost() {
    return socksdBindHost;
  }

  public void setSocksdBindHost(String socksdBindHost) {
    this.socksdBindHost = socksdBindHost;
  }

  public String getHttpProxyBindHost() {
    return httpProxyBindHost;
  }

  public void setHttpProxyBindHost(String httpProxyBindHost) {
    this.httpProxyBindHost = httpProxyBindHost;
  }
  
  public Integer getStartingHttpProxyPort() {
    return startingHttpProxyPort;
  }

  public void setStartingHttpProxyPort(Integer startingHttpProxyPort) {
    this.startingHttpProxyPort = startingHttpProxyPort;
  }

  public Properties getClientProps() {
    return clientProps;
  }

  public Boolean getUseSsl() {
    return useSsl;
  }

  public void setUseSsl(Boolean useSsl) {
    this.useSsl = useSsl;
  }

  public String getSslKeyStorePassword() {
    return sslKeyStorePassword;
  }

  public String getSslKeyStoreFile() {
    return sslKeyStoreFile;
  }

  /*
   * End Getters and Setters.
   */

  /**
   * parses the resourceRule (of pattern   <rule> <allowed entities>)
   * into a String[] with 1st string = <rule> 
   * and 2nd string = <allowed entities>
   * 
   * If either of the above 2 are not found in the resourceRule, ResourceConfigException is thrown
   * 
   * @param resourceRule
   * @return String[] with rule and allowed entities in 2 different strings
   * @throws ResourceConfigException if either of the above 2 are not found
   */
  public static String[] parseResourceRule(String resourceRule) throws ResourceConfigException {
    String[] resourceStr = resourceRule.split("[\\s]+", 2);
    if (resourceStr.length < 2) {
      throw new ResourceConfigException("Expected but not found both pattern & allowed entities " +
          " in this rule: " + resourceRule);
    }
    return resourceStr;
  }
}
