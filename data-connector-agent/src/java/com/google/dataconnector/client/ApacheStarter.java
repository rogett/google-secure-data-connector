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
package com.google.dataconnector.client;

import com.google.dataconnector.client.SecureDataConnection.KillProcessShutdownHook;
import com.google.dataconnector.util.ApacheHelper;
import com.google.dataconnector.util.ApacheSetupException;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Sets up apache configuration for each of the rules and starts Apache's httpd.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheStarter extends Thread {

  // Logging instance
  private static final Logger LOG = Logger.getLogger(ApacheStarter.class);
  
  private LocalConf localConf;
  private ApacheHelper apacheHelper;
  private Runtime runtime;

  /**
   * Creates the ApacheStarter with injected dependencies.
   * 
   * @param localConf local agent configuration.
   * @param apacheHelper apache helper object.
   * @param runtime system runtime.
   */
  @Inject 
  public ApacheStarter(LocalConf localConf, ApacheHelper apacheHelper, Runtime runtime) {
    this.localConf = localConf;
    this.apacheHelper = apacheHelper;
    this.runtime = runtime;
  }
  
  /**
   * With template creates proxy match rules for each resource rule and starts httpd using
   * apachectl.
   */
  public void startApacheHttpd() throws ApacheSetupException {
    LOG.info("Configuring httpd");
    apacheHelper.generateHtpasswdFiles();
    apacheHelper.generateHttpdConf();
    start();
  }
  
  /**
   * Starts Apache's HTTPD in a new thread and waits for it to exit.  All startup errots are 
   * logged.
   */
  @Override
  public void run() {
    LOG.info("Starting httpd");
    try {
      String[] commandLine = new String[] {
          localConf.getApacheRoot() + File.separator + "bin" + File.separator + "httpd",
          "-D", "FOREGROUND",
          "-f", ApacheHelper.getHttpdConfFileName(localConf)
      };
      Process p = runtime.exec(commandLine);
      runtime.addShutdownHook(new KillProcessShutdownHook(p));
      
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String errorLine;
      
      // Catch any startup errors
      while ((errorLine = br.readLine()) != null) {
        LOG.info("httpd output: " + errorLine);
      }

    } catch (IOException e) {
      LOG.log(Level.ERROR, "Apache did not start correctly.", e);
      throw new RuntimeException(e);
    }
  }
}
