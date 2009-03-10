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
package com.google.dataconnector.client;

import com.google.dataconnector.util.ApacheHelper;
import com.google.dataconnector.util.ApacheSetupException;
import com.google.dataconnector.util.LocalConf;
import com.google.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Sets up apache configuration for each of the rules and starts Apache's httpd.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheStarter {

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
    apacheHelper.generateHttpdConf();
    // We cleanup existing httpds if they are still running.
    LOG.info("Shutting down any existing httpd processes");
    runApacheCtl(ApacheCommand.STOP);
    LOG.info("Starting httpd process");
    runApacheCtl(ApacheCommand.START);
    // Set apache to stop on shutdown.
    runtime.addShutdownHook(new Thread() {
      @Override
      public void run() {
        runApacheCtl(ApacheCommand.STOP);
      }
    });
  }
  
  /**
   * Runs apachectl with specified command with configured apache configuration file.
   * 
   * @param command the apache command to run.
   */
  public void runApacheCtl(ApacheCommand command) {
    LOG.info("apachectl " + command + " issued.");
    try {
      String[] commandLine = new String[] {
          localConf.getApacheCtl(),
          "-f", ApacheHelper.getHttpdConfFileName(localConf),
          "-k", command.toString()
      };
      Process p = runtime.exec(commandLine);
      
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String errorLine;
      
      // Catch any startup errors
      while ((errorLine = br.readLine()) != null) {
        LOG.info("apachectl: " + errorLine);
      }
    } catch (IOException e) {
      LOG.log(Level.ERROR, "Apache did not start correctly.", e);
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Enum to represent different apachectl directives.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  enum ApacheCommand {
    STOP("stop"),
    START("start");
    
    private String command;
    
    private ApacheCommand(String command) {
      this.command = command;
    }
    
    @Override
    public String toString() {
      return command; 
    }
  }
}
