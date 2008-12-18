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

import java.io.File;

/**
 * Validates the local configuration bean.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class LocalConfValidator {
  
  private FileFactory fileFactory;
  
  /**
   * Builds a validator with pre-configured dependencies.
   */
  public LocalConfValidator() {
    this(new FileFactory());
  }
  
  /**
   * Constructs the validator with dependencies injected.
   * 
   * @param fileFactory the factory used to get new {@link File} instances.
   */
  public LocalConfValidator(FileFactory fileFactory) {
    this.fileFactory = fileFactory;
  }
  
  private static final int MAX_PORT = 65535;

  /**
   * With the supplied filename, this method calls {@link File#canRead()}.
   * 
   * @param configKey the config file key associated with the supplied file name.
   * @param filename the filename to check for read access
   * @throws LocalConfException if {@link File#canRead()} fails and will contain a message
   * associated the file with the supplied config file key.
   */
  private String canReadFile(String configKey, String filename) throws LocalConfException {
    File file = fileFactory.getFile(filename);
    if (!file.canRead()) {
      return("Cannot read " + configKey + "file: " + filename + "\n");
    }
    return "";
  }
  
  /**
   * Validates the LocalConf bean using a series of semantic checks.
   * 
   * @param localConf the bean to validate.
   * @throws LocalConfException if any config entry is invalid.  This code batches up
   * all errors and throws them at once for improved user experience.
   */
  public void validate(LocalConf localConf) throws LocalConfException {
    
    StringBuilder errors = new StringBuilder();
    
    // rulesFile
    String rulesFile = localConf.getRulesFile();
    if (rulesFile != null) {
      errors.append(canReadFile("rulesFile", rulesFile));
    } else {
      errors.append("'rulesFile' required\n");
    }
    
    // sdcServerHost
    if (localConf.getSdcServerHost() == null) {
      errors.append("'sdcServerHost' required\n");
    }
    
    // sdcServerPort
    Integer sdcServerPort = localConf.getSdcServerPort();
    if (sdcServerPort != null) {
      if (sdcServerPort > MAX_PORT || sdcServerPort < 0) {
        errors.append("invalid 'sdcServerPort': " + sdcServerPort);
      }
    } else {
      errors.append("'sdcServerPort' required\n");
    }
    
    // domain
    if (localConf.getDomain() != null) {
      /*
       * Regex verifies the following.  Credit: 
       * http://www.martienus.com/code/regex-validate-e-mail-address-in-php.html
       * 
       * Sub-domain name may only contain letters, digits and hyphen
       * Multiple sub-domains are permitted
       * Domain name may only contain letters, digits and hyphen
       * Domain name and sub-domain name may not begin and/or end with a hyphen
       * Domain name and sub-domain must be between 2 to 63 characters long
       * Top-level domain may only contain letters
       * Top-level domain must be between 2 to 6 characters long
       * Sub-domain names, the domain name and the top-level domain name are separated by 
       * single periods '.'
       */
      if (!localConf.getDomain().matches("^(([A-z0-9]+\\-?[A-z0-9]+)+\\.)+[A-z]{2,6}$")) {
        errors.append("'domain' " + localConf.getDomain() + " not valid\n");
      }
    } else {
      errors.append("'domain' required\n");
    }
    
    // user
    if (localConf.getUser() != null) {
      if (localConf.getUser().matches("\\s")) {
        errors.append("'user' contains spaces\n");
      }
    } else {
      errors.append("'user' required\n");
    }
    
    // oauthKey or password required
    if (localConf.getOauthKey() != null) {
      localConf.setAuthType(AuthRequest.AuthType.OAUTH);
    } else if (localConf.getPassword() != null) {
      localConf.setAuthType(AuthRequest.AuthType.PASSWORD);
    } else {
      errors.append("'oauthKey' or 'password' required\n");
    }
    
    // useSsl
    if (localConf.getUseSsl() == null) {
      errors.append("'useSsl' required\n");
    }
    
    // sslKeyStoreFile
    if (localConf.getSslKeyStoreFile() != null) {
      errors.append(canReadFile("sslKeyStoreFile", localConf.getSslKeyStoreFile()));
    } 
    
    // sslKeyStorePassword 
    if (localConf.getSslKeyStoreFile() != null && localConf.getSslKeyStorePassword() == null) {
        errors.append("'sslKeyStorePassword' required\n");
    }
    
    // clientId
    if (localConf.getClientId() != null) {
      if (localConf.getClientId().matches("\\s")) {
        errors.append("'clientId' contains spaces\n");
      }
    } else {
      errors.append("'clientId' required\n");
    }
    
    // sshd
    if (localConf.getSshd() != null) {
      errors.append(canReadFile("sshd", localConf.getSshd()));
    } else {
      errors.append("'sshd' required\n");
    }
    
    // startingHttpProxyPort 
    Integer httpProxyPort = localConf.getHttpProxyPort();
    if (httpProxyPort != null) {
      if (httpProxyPort > MAX_PORT || httpProxyPort < 0) {
        errors.append("invalid 'startingHttpProxyPort': " +  httpProxyPort);
      }
    } else {
      errors.append("'httpProxyPort' required\n");
    }
    
    // httpProxyBindHost
    if (localConf.getHttpProxyBindHost() != null) {
      if (localConf.getHttpProxyBindHost().matches("\\s")) {
        errors.append("'httpProxyBindHost' contains spaces\n");
      }
    } else {
      errors.append("'httpProxyBindHost' required\n");
    }
    
    // socksServerPort 
    Integer socksServerPort = localConf.getSocksServerPort();
    if (socksServerPort != null) {
      if (socksServerPort > MAX_PORT || socksServerPort < 0) {
        errors.append("invalid 'socksServerPort': " +  socksServerPort);
      }
    } else {
      errors.append("'socksServerPort' required\n");
    }
    
    // sockdBindHost
    if (localConf.getSocksdBindHost() != null) {
      if (localConf.getSocksdBindHost().matches("\\s")) {
        errors.append("'socksdBindHost' contains spaces\n");
      }
    } else {
      errors.append("'socksdBindHost' required\n");
    }
    
    // logProperties 
    if (localConf.getLogProperties() == null) {
      errors.append("'logProperties' required\n");
    }
    
    // socksProperties 
    if (localConf.getSocksProperties() == null) {
      errors.append("'socksProperties' required\n");
    }
    
    // apache htpasswd
    if (localConf.getApacheHtpasswd() != null) {
      errors.append(canReadFile("apacheHtpasswd", localConf.getApacheHtpasswd()));
    } else {
      errors.append("'apacheHtpasswd' required.");
    }
    
    // apachectl
    if (localConf.getApacheCtl() != null) {
      errors.append(canReadFile("apacheCtl", localConf.getApacheCtl()));
    } else {
      errors.append("'apacheCtl' required.");
    }
    
    // apache conf dir
    if (localConf.getApacheConfDir() != null) {
      errors.append(canReadFile("apacheConfDir", localConf.getApacheConfDir() + File.separator +
          LocalConf.HTTPD_CONF_TEMPLATE_FILE));
    } else {
      errors.append("'apacheConfDir' required.");
    }
    
    // Check for errors and throw
    if (errors.length() > 0) {
      throw new LocalConfException(errors.toString());
    }
  }
  
  /**
   * Factory so we can dependency inject the File object.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public static class FileFactory {
    public File getFile(String fileName) {
      return new File(fileName);
    }
  }
}
