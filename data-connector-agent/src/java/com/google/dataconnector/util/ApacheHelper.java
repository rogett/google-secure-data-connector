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

import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
  static final String PROXY_MATCH_RULE_TEMPLATE = "<ProxyMatch __PATTERN__>\n" +
      "order allow,deny\n" +
      "allow from all\n" +
      "deny from none\n" +
      "AuthType basic\n" +
      "AuthName \"woodstock proxy\"\n" +
      "AuthBasicAuthoritative On\n" +
      "AuthBasicProvider file\n" +
      "AuthUserFile __HTPASSWDFILE__\n" +
      "Require valid-user\n" +
      "</ProxyMatch>\n";
  
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
      httpdConf = httpdConf.replace("_PROXYMATCHRULES_", makeProxyMatchConfEntries());
      httpdConf = httpdConf.replace("_APACHE_PORT_", localConf.getHttpProxyPort().toString());
      httpdConf = httpdConf.replace("_APACHE_BIND_HOST_", localConf.getHttpProxyBindHost());
      
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
   * For each http resource rule, we take the template {@link #PROXY_MATCH_RULE_TEMPLATE} and 
   * substitute the correct value based on the resource configuration.  We build up the entire
   * &lt;ProxyMatch&gt; section and return it.
   * 
   * @return a string representing all the "&lt;ProxyMatch&gt;" rules.
   */
  String makeProxyMatchConfEntries() {

    StringBuilder proxyMatchEntries = new StringBuilder();
    
    // sort the resource rules by sequence number
    Collections.sort(resourceRules);
    // reverse the rules as Apache HTTP processes ProxyMatch rules in a reverse order.
    // see http://httpd.apache.org/docs/2.0/sections.html for how precedence in Apache works.
    Collections.reverse(resourceRules);
    
    // Create ProxyMatch entries for each rule.
    for (ResourceRule resourceRule : resourceRules) {
      LOG.info("Creating <ProxyMatch> section for resource " + resourceRule.getName());
      // Skip non http rules
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) { 
        continue;
      }

      // Substitute 
      String proxyMatchRule = new String(PROXY_MATCH_RULE_TEMPLATE);
      proxyMatchRule = proxyMatchRule.replace("__PATTERN__", resourceRule.getPattern());
      proxyMatchRule = proxyMatchRule.replace("__HTPASSWDFILE__", 
          makeHtpasswdPath(localConf, resourceRule.getName()));
      proxyMatchEntries.append(proxyMatchRule);
      proxyMatchEntries.append("\n");
    }
    return proxyMatchEntries.toString();
  }
  
  /**
   * Create an htpassword file for each rule with its username as the 
   * {@link #DEFAULT_PROXY_AUTH_USER} and the password as the resourceRule secret key.
   * 
   * @throws ApacheSetupException if any errors occur running htpasswd binary.
   */
  public void generateHtpasswdFiles() throws ApacheSetupException {

    try {
      for (ResourceRule resourceRule : resourceRules) {
        // Skip non http rules
        if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) { 
          continue;
        }
        
        // Get path and remove any existing files.
        String htpasswdFile = makeHtpasswdPath(localConf, resourceRule.getName());
        fileUtil.deleteFile(htpasswdFile);
        
        // Run htpassword to create new file for this resource.
        LOG.info("creating password " + resourceRule.getSecretKey() + " for rule " + 
            resourceRule.getPattern());
        Process p = runtime.exec(new String[] { 
            localConf.getApacheRoot() + File.separator + HTPASSWD_BINARY,
            "-b",
            "-c",
            htpasswdFile,
            DEFAULT_PROXY_AUTH_USER,
            resourceRule.getSecretKey().toString() });
        
        // Log any errors.
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String errorLine;
        while ((errorLine = br.readLine()) != null) {
          LOG.error("htpasswd error: " + errorLine);
        }
        
        // Wait until htpassword finishes
        p.waitFor(); 
        // Schedule this temporary file for deletion on vm exit.
        fileUtil.deleteFileOnExit(htpasswdFile);
      }
    } catch (IOException e) {
      throw new ApacheSetupException(e);
    } catch (InterruptedException e) {
      throw new ApacheSetupException(e);
    }
  }
  
  /**
   * Returns actual apache configuration filename based on constants and server apache conf dir.
   * 
   * @return path used for apache configuration file.
   */
  public static String getHttpdConfFileName(LocalConf localConf) {
      return localConf.getApacheConfDir() + File.separator + HTTP_CONF_FILE_NAME;
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
    return localConf.getApacheConfDir() + File.separator + HTPASSWD_FILE_PREFIX + name;
  }
}
