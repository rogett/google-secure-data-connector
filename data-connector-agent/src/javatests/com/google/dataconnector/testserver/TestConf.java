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
package com.google.dataconnector.testserver;

import com.google.feedserver.util.ConfigFile;
import com.google.feedserver.util.Flag;

/**
 * All the command line flags to the testserver are listed here. Some of the flags for
 * consumption by the testserver itself while the others are to be passed on to the
 * server and client processes started up as part of testing.
 * 
 * @author vnori@google.com (Vasu Nori)
 */
public class TestConf {

  public static enum ExitLabel {
    NORMAL_EXIT("Server exits normally"),
    AFTER_CLIENT_CONNECTS("Server dies right after connecting with client"),
    AFTER_AUTHZ_REQ_RECVD("Server dies right after receiving the Authz request"),
    AFTER_AUTHZ_RESPONSE_SENT("Server dies right after sending Authz response"),
    AFTER_REG_REQ_RECVD("Server dies right after receiving the Registration request"),
    AFTER_REG_RESPONSE_SENT("Server dies right after sending Registration response"),
    AFTER_SSHD_START("Server dies right after starting SSHD"),
    UNKNOWN("UNKNOWN state");
    
    private String desc;
    private ExitLabel(String s) {
      this.desc = s;
    }
    
    public String getDesc() {
      return desc;
    }
  }

  private ExitLabel exitLabel = ExitLabel.NORMAL_EXIT;
  private ExitLabel actualExitLabel = ExitLabel.UNKNOWN;

  @ConfigFile(required = true)
  @Flag(help = "Configuration File")
  private String localConfigFile = ""; //Configure flag to be of type "String"

  @ConfigFile(required = true)
  @Flag(help = "Agent firewall rules configuration file.")
  private String rulesFile;
  
  @Flag(help = "exit point - where the server should exit at ")
  private String exitpoint;
  
  @Flag(help = "server socket listen port")
  private int testServerListenPort = 0; 
  
  // Private key to authenticate against the SSHD
  @Flag(help = "The private key to use for authentication")
  private String sshPrivateKeyFile;
  
  @Flag(help = "debug level")
  private boolean verbose = false;
  
  public String getSshPrivateKeyFile() {
    return sshPrivateKeyFile;
  }
  public void setSshPrivateKeyFile(String sshPrivateKeyFile) {
    this.sshPrivateKeyFile = sshPrivateKeyFile;
  }
  public int getTestServerListenPort() {
    return testServerListenPort;
  }
  public void setTestServerListenPort(int testServerListenPort) {
    this.testServerListenPort = testServerListenPort;
  }
  public String getLocalConfigFile() {
    return localConfigFile;
  }
  public void setLocalConfigFile(String localConfigFile) {
    this.localConfigFile = localConfigFile;
  }
  public String getRulesFile() {
    return rulesFile;
  }
  public void setRulesFile(String rulesFile) {
    this.rulesFile = rulesFile;
  }
  public String getExitpoint() {
    return exitpoint;
  }
  
  public void setExitpoint(String exitAt) {
    exitpoint = exitAt;
    // validate the exitPoint arg
    try {
      exitLabel = ExitLabel.valueOf(exitpoint);
    } catch (IllegalArgumentException e) {
      System.out.println("incorrect value for exitPoint arg");
      System.exit(-1);
    }  
  }
  public ExitLabel getExitLabel() {
    return exitLabel;
  }
  public ExitLabel getActualExitLabel() {
    return actualExitLabel;
  }
  public void setActualExitLabel(ExitLabel actualExitLabel) {
    this.actualExitLabel = actualExitLabel;
  }
  public boolean isVerbose() {
    return verbose;
  }
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
}
