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

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.dataconnector.registration.v2.AuthResponse;
import com.google.dataconnector.registration.v2.RegistrationResponse;
import com.google.dataconnector.registration.v2.RegistrationRequest;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.AuthenticationException;
import com.google.dataconnector.util.RegistrationException;
import com.google.inject.Inject;

import java.net.Socket;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class to handle client registration to the Data Firewall server.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientRegistrationUtil {

  //Logging instance
  private static final Logger LOG = Logger.getLogger(ClientRegistrationUtil.class);
  
  // Injected Dependencies.
  private RegistrationRequest registrationRequest;
  private AuthRequest authRequest;
  
  /**
   * Creates the client registration util with injected dependencies.
   * 
   * @param authRequest dependency.
   * @param registrationRequest dependency.
   */
  @Inject
  public ClientRegistrationUtil(AuthRequest authRequest, RegistrationRequest registrationRequest) {
    this.authRequest = authRequest;
    this.registrationRequest = registrationRequest;
  }
      
  public static final String INITIAL_HANDSHAKE_MSG = "v2.2 " + 
      ClientRegistrationUtil.class.getPackage().getImplementationVersion();
 
  /**
   * Authorizes this Secure Link connection from the info specified in the Client Configuration
   * object. 
   *  
   * @param socket The connected socket.
   * @param localConf the local configuration object.
   * @returns the AuthRequest packet associated with this connection.
   * @throws AuthenticationException if authorization fails 
   * @throws IOException if socket communication errors occur.
   */
  public AuthRequest authorize(final Socket socket, 
      LocalConf localConf) throws AuthenticationException, IOException {

    String userEmail = localConf.getUser() + "@" + 
    localConf.getDomain();
    LOG.info("Attempting login for " + userEmail);

    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    try {
      // send a message to initiate handshake with tunnelserver
      LOG.info("Sending initial handshake msg");
      pw.println(INITIAL_HANDSHAKE_MSG);
      pw.flush();
      
      if (localConf.getAuthType() == AuthRequest.AuthType.PASSWORD) {
        authRequest.setUser(localConf.getUser());
        authRequest.setDomain(localConf.getDomain());
        LOG.info("Sending login packet: " + authRequest.toJson().toString());
       // set the password now - to avoid having it printed in the above step
        authRequest.setPassword(localConf.getPassword());
      } else {
        // shouldn't have come here because LocalConfValidator should have thrown error
        throw new AuthenticationException("Unsupported AuthType specified. Check localConfig.");
      }
      
      // send auth request packet
      pw.println(authRequest.toJson().toString());
      pw.flush();
      
      // wait for response and check.
      LOG.debug("Reading auth response");
      String jsonResponseString = br.readLine();
      if (jsonResponseString == null ||
          jsonResponseString.trim().length() == 0) {
        throw new AuthenticationException("No Authorization response recvd. exiting.");
      }
      LOG.debug("Got an auth response");
      
      String email = localConf.getUser() + "@" + localConf.getDomain();
      AuthResponse authResponse = new AuthResponse(new JSONObject(jsonResponseString));
      if(authResponse.getStatus() != AuthResponse.Status.OK) {
        throw new AuthenticationException("Authentication Failed for " + email + ": " + 
            authResponse.getStatus());
      }
      LOG.info("Login for " + email + " successful");
      return authRequest;
    } catch (JSONException e) {
      throw new AuthenticationException("Mangled JSON response during auth", e);
    }
  }
  
  /**
   * Send registration request which includes connection information (secure key and connection
   * rules).  When the server receives this request it registers the connection info with for
   * the domain this client connection is bound to.  
   * 
   * @param socket the connected socket.
   * @param authRequest the auth request for this connection.
   * @param resourceRules the rule set.
   * @throws RegistrationException if the registration fails.  This can happen if the server has 
   *             backend issues.
   * @throws IOException if any socket communication issues occur.
   */
  public void register(final Socket socket, final AuthRequest authRequest, 
      List<ResourceRule> resourceRules) throws RegistrationException, IOException {

    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader br = new BufferedReader(
        new InputStreamReader(socket.getInputStream()));

    try {
      // send registration request packet
      registrationRequest.populateFromResources(resourceRules);
      LOG.info("Registering rules: " + registrationRequest.toJson().toString());
      pw.println(registrationRequest.toJson().toString());
      pw.flush();
    
      // Receive registration response packet
      LOG.debug("Waiting for registration response");
      String jsonResponseString = br.readLine();
      if (jsonResponseString == null ||
          jsonResponseString.trim().length() == 0) {
        throw new RegistrationException("No Registration response recvd. exiting.");
      }
      LOG.debug("Read registration response: " + jsonResponseString);

      RegistrationResponse regResponse = new RegistrationResponse(
          new JSONObject(jsonResponseString));

      if (regResponse.getStatus() != RegistrationResponse.Status.OK) {
        throw new RegistrationException("Registration Failed" + regResponse.getStatus() + 
            (regResponse.getErrorMsg() != null ? ": " + regResponse.getErrorMsg() : "."));
      }
      LOG.info("Registration successful");
    } catch (JSONException e) {
      throw new RegistrationException("Mangled JSON response during registration", e);
    } catch (ResourceException e) {
      throw new RegistrationException("Invalid Resources ", e);
    }
  }
}
