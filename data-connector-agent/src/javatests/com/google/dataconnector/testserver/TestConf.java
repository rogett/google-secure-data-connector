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
