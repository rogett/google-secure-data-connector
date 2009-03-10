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

import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods that generate Apache Configuration and make htpassword files from  Secure Data 
 * Client configuration.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheHelper {
  
  // Logging instance
  private static final Logger LOG = Logger.getLogger(ApacheHelper.class);
  
  // Constants 
  /** 
   * This is the username we expect the server side to authenticate as.  Currently this is hard
   * coded as "foo" as its not used inside of apache.  Changing this requires a serverside change!
   */
  static final String DEFAULT_PROXY_AUTH_USER = "foo";
  static final String HTPASSWD_BINARY = "bin" + File.separator + "htpasswd";
  static final String HTPASSWD_FILE_PREFIX = "htpasswd.resource.";
  static final String HTTP_CONF_FILE_NAME = "sdc.httpd.conf";
  
  static final String PROXY_RULE_TEMPLATE_20 = "" +
  	  "<VirtualHost __BIND_HOST__:__PORT__ >\n" +
  	  "DocumentRoot __DOCUMENT_ROOT__\n" +
  	  "ProxyRequests on\n" +
  	  "ProxyVia on\n" +
  	  "<Proxy __PATTERN__>\n" +
      "  order allow,deny\n" +
      "  allow from all\n" +
      "  deny from none\n" +
      "</Proxy>\n" +
      "</VirtualHost>\n";
  
  // For now the templates are the same between versions, however, this certainly isn't guaranteed
  // going forward.
  static final String PROXY_RULE_TEMPLATE_22 = PROXY_RULE_TEMPLATE_20;
  
  /**
   * Valid apache versions.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  enum ApacheVersion {
    TWOTWO,
    TWOZERO,
    INVALID
  }
  
  // Dependencies
  private LocalConf localConf;
  private List<ResourceRule> resourceRules;
  private Runtime runtime;
  private FileUtil fileUtil;
  
  /**
   * Creates apache helper with provided dependencies.
   * 
   * @param localConf client configuration object.
   * @param resourceRules resource rules being shared by this agent.
   * @param runtime current runtime used to start apache processes.
   * @param fileUtil file manipulation utility.
   */
  @Inject
  public ApacheHelper(LocalConf localConf, List<ResourceRule> resourceRules, Runtime runtime,
      FileUtil fileUtil) {
    this.localConf = localConf;
    this.resourceRules = resourceRules;
    this.runtime = runtime;
    this.fileUtil = fileUtil;
  }
  
  /**
   * Runs the apache binary specified in the {@link LocalConf} to determine its version. 
   * 
   * @return the version in enum form.
   * @throws ApacheSetupException if the apache binary cannot be executed or read from.
   */
  ApacheVersion getApacheVersion() throws ApacheSetupException {
    try {
      // Read in the version from the binary.
      Process p;
      p = runtime.exec(new String[] { localConf.getApacheCtl(), "-v" });
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String version = "";
      while ((version = br.readLine()) != null) {
        if (version.matches(".*Server version.*")) {
          break;
        }
      }
      p.destroy();
      
      // Check to see we got a version line.
      if (version == null) {
        LOG.warn("Apache version call returned null!");
        return ApacheVersion.INVALID;
      }
      
      // Figure out the version.
      // eg: Server version: Apache/2.2.6 (Unix)
      if (version.matches(".*/2\\.2\\..*")) {
        return ApacheVersion.TWOTWO;
      } else if (version.matches(".*/2\\.0\\..*")) {
        return ApacheVersion.TWOZERO;
      } else {
        LOG.warn("Apache version: " + version + " not valid");
        return ApacheVersion.INVALID;
      }
    } catch (IOException e) {
      throw new ApacheSetupException(e);
    }
    
  }
  /**
   * Uses httpd.conf template and substitutes in ProxyMatch and htpassword paths.  It also
   * writes out the final httpd.conf to the file to the filesystem for Apache Httpd to read.
   * 
   * @throws ApacheSetupException if any file IO errors occur when writing the httpd conf.
   */
  public void generateHttpdConf() throws ApacheSetupException {
    
    try {
      // Read Template
      String httpConfTemplateFilename = getHttpdConfTemplateFileName(localConf);
      String httpdConf = fileUtil.readFile(httpConfTemplateFilename);
      
      // Perform Replacement
      httpdConf = httpdConf.replace("_PID_FILE_", localConf.getApacheConfDir() + File.separator +
          "httpd.pid-" + localConf.getClientId());
      httpdConf = httpdConf.replace("_PROXYMATCHRULES_", makeProxyMatchConfEntries());
      httpdConf = httpdConf.replace("_LISTENENTRIES_", makeListenEntries());
      
      // Write new httpconf
      String httpConfFilename = getHttpdConfFileName(localConf);
      fileUtil.deleteFile(httpConfFilename);
      fileUtil.writeFile(httpConfFilename, httpdConf);
      fileUtil.deleteFileOnExit(httpConfFilename);
    } catch (IOException e) {
      throw new ApacheSetupException(e);
    }
  }
  
  /**
   * Creates "Listen" entries for httpd conf.
   * 
   * @return all "Listen" ip:port entries needed for this rule configuration.
   */
  String makeListenEntries() {
    StringBuilder listenConfigEntries = new StringBuilder();
    for (ResourceRule resourceRule : resourceRules) {
      // We only create entries for URLEXACT type patterns.
      if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        listenConfigEntries.append("Listen " + localConf.getHttpProxyBindHost() + ":" + 
            resourceRule.getHttpProxyPort() +"\n");
      }
    }
    return listenConfigEntries.toString();
  }
  
  
  /**
   * For each http resource rule, we take the template {@link #PROXY_RULE_TEMPLATE_20}
   * substitute the correct value based on the resource configuration.  We build up the entire
   * &lt;ProxyMatch&gt; section and return it.
   * 
   * @return a string representing all the "&lt;ProxyMatch&gt;" rules.
   */
  String makeProxyMatchConfEntries() throws ApacheSetupException {

    StringBuilder proxyMatchEntries = new StringBuilder();
    
    // sort the resource rules by sequence number
    Collections.sort(resourceRules);
    // reverse the rules as Apache HTTP processes ProxyMatch rules in a reverse order.
    // see http://httpd.apache.org/docs/2.0/sections.html for how precedence in Apache works.
    Collections.reverse(resourceRules);
    
    // Create Proxy entries for each rule.
    for (ResourceRule resourceRule : resourceRules) {
      // Skip non HTTP rules
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        continue;
      }
      
      LOG.info("Creating <Proxy> section for resource " + resourceRule.getRuleNum());
      // Get apache version from apache binary then select appropriate template.
      String proxyMatchRule = "";
      switch(getApacheVersion()) {
        case TWOTWO:
          proxyMatchRule = PROXY_RULE_TEMPLATE_22;
          break;
        case TWOZERO:
          proxyMatchRule = PROXY_RULE_TEMPLATE_20;
          break;
        default:
          throw new ApacheSetupException("Unsupported apache version");
      }
        
      // Add sequence number to config file for debugging.
      proxyMatchRule = "# Sequence Number " + resourceRule.getRuleNum() + "\n" + proxyMatchRule;
      proxyMatchRule = proxyMatchRule.replace("__DOCUMENT_ROOT__", localConf.getApacheConfDir() + 
          "/htdocs");
      if (resourceRule.getPatternType().equals(ResourceRule.HOSTPORT)) { 
        // For HOSTPORT type, we allow any URL path.
        proxyMatchRule = proxyMatchRule.replace("__PATTERN__", resourceRule.getPattern() + "*");
      } else { 
        // For URLEXACT we only allow what is explicitly listed.  
        // TODO(rayc) determine behavior of trailing slashes.
        proxyMatchRule = proxyMatchRule.replace("__PATTERN__", resourceRule.getPattern());
      }
      proxyMatchRule = proxyMatchRule.replace("__SEQNUM__", Integer.toString(
          resourceRule.getRuleNum())); 
      proxyMatchRule = proxyMatchRule.replace("__BIND_HOST__", localConf.getHttpProxyBindHost());
      proxyMatchRule = proxyMatchRule.replace("__PORT__", 
          resourceRule.getHttpProxyPort().toString());
      proxyMatchEntries.append(proxyMatchRule);
      proxyMatchEntries.append("\n");
    }
    return proxyMatchEntries.toString();
  }
  
  /**
   * Returns actual apache configuration filename based on constants and server apache conf dir.
   * 
   * @return path used for apache configuration file.
   */
  public static String getHttpdConfFileName(LocalConf localConf) {
    try {
      return localConf.getApacheConfDir() + File.separator + HTTP_CONF_FILE_NAME +
          "." + URLEncoder.encode(localConf.getClientId(), "UTF8");
    } catch (UnsupportedEncodingException e) {
      // meh, we only catch this because URLEncoder is dumb and throws a checked exception for
      // what is programmer error.
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns apache configuration template filename based on constants and server apache conf dir.
   * 
   * @return path used for apache configuration file.
   */
  static String getHttpdConfTemplateFileName(LocalConf localConf) {
    return localConf.getApacheConfDir() + File.separator + LocalConf.HTTPD_CONF_TEMPLATE_FILE;
  }
  
  /**
   * Creates the htpasswd file location from the given resource rule.
   * 
   * @param name the unique identifier for this htpasswd file.
   * @return a constructed path name.
   */
  static String makeHtpasswdPath(LocalConf localConf, String name) {
    try {
      return localConf.getApacheConfDir() + File.separator + HTPASSWD_FILE_PREFIX + name + "." +
          URLEncoder.encode(localConf.getClientId(), "UTF8");
    } catch (UnsupportedEncodingException e) {
      // meh, we only catch this because URLEncoder is dumb and throws a checked exception for
      // what is programmer error.
      throw new RuntimeException(e);
    }
  }
}
