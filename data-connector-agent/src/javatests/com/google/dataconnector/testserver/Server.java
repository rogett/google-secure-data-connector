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

import com.google.dataconnector.client.Client;
import com.google.dataconnector.registration.v2.AuthResponse;
import com.google.dataconnector.registration.v2.RegistrationResponse;
import com.google.dataconnector.util.ConnectionException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Sets up a test server for testing the client changes.
 * 
 * @author vnori@google.com (Vasu Nori)
 */
public class Server extends Thread {
  private static final Logger LOG = Logger.getLogger(TestServerFailures.class);

  private boolean sshdStarted = false;
  private SshClient sshClient;
  private TestConf testConf;
  private Socket clientSocket;
  
  public Server(TestConf testConf) {
    this.testConf = testConf;  
  }
  
  @Override
  public void run() {
    // Bootstrap logging system
    PropertyConfigurator.configure(Client.getBootstrapLoggingProperties());

    try {
      // start listening on a socket for client connections
      ServerSocket serverSocket = new ServerSocket(testConf.getTestServerListenPort());
      clientSocket = serverSocket.accept();
      LOG.info("accepted client socket. ");
      if (shouldIExit(TestConf.ExitLabel.AFTER_CLIENT_CONNECTS)) {
        return;
      }
      
      // read hello string
      BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String clientHelloString = br.readLine();
      LOG.info("Read 1st line from Client: " + clientHelloString);
      
      // read auth request
      String authRequestJsonString = br.readLine();
      LOG.info("Read Auth request from Client: " + authRequestJsonString);
      if (shouldIExit(TestConf.ExitLabel.AFTER_AUTHZ_REQ_RECVD)) {
        return;
      }
      
      // send auth response
      AuthResponse authResponse = new AuthResponse();
      authResponse.setStatus(AuthResponse.Status.OK);
      LOG.info("Sending auth response " + authResponse.toJson().toString());
      sendResponse(authResponse.toJson().toString(), clientSocket);
      if (shouldIExit(TestConf.ExitLabel.AFTER_AUTHZ_RESPONSE_SENT)) {
        return;
      }
      
      // read registration request
      String regJsonString = br.readLine();
      LOG.info("Read Reg request from Client: " + regJsonString);
      if (shouldIExit(TestConf.ExitLabel.AFTER_REG_REQ_RECVD)) {
        return;
      }
      
      // send reg response
      RegistrationResponse regResponse = new RegistrationResponse();
      regResponse.setStatus(RegistrationResponse.Status.OK);
      LOG.info("sending reg response " + regResponse.toJson().toString());
      sendResponse(regResponse.toJson().toString(), clientSocket);
      if (shouldIExit(TestConf.ExitLabel.AFTER_REG_RESPONSE_SENT)) {
        return;
      }
      
      // start SSHD
      // open a socket for portforwarding of SSH socket
      Socket localForwardPortSocket = new Socket();
      sshClient = new SshClient(testConf.getSshPrivateKeyFile());
      sshClient.connect(clientSocket, 20500, 1080); // get this as an arg
      sshdStarted = true;
      LOG.info("SSH started");
      if (shouldIExit(TestConf.ExitLabel.AFTER_SSHD_START)) {
        return;
      }

      LOG.info("Server exiting");
    } catch (IOException e) {
      LOG.fatal(e.getMessage(), e);
    } catch (JSONException e) {
      LOG.fatal(e.getMessage(), e);
    } catch (ConnectionException e) {
      LOG.fatal(e.getMessage(), e);
    } finally {
      if (sshdStarted) {
        sshClient.close();
      }
    }
    
    // normal exit
    testConf.setActualExitLabel(TestConf.ExitLabel.NORMAL_EXIT);
  }
  
  private void sendResponse(String response, Socket socket) throws IOException {
    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    pw.println(response);
    pw.flush();
  }
  
  private boolean shouldIExit(TestConf.ExitLabel here) throws IOException {
    if (testConf.getExitLabel() == here) {
      LOG.info("exiting - according to input arg " + testConf.getExitLabel());
      testConf.setActualExitLabel(testConf.getExitLabel());
      clientSocket.close();
      return true;
    }
    return false;
  }
}
