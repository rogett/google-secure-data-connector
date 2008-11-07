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

import com.google.dataconnector.util.AuthRequest;
import com.google.dataconnector.util.AuthResponse;
import com.google.dataconnector.util.AuthenticationException;
import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.MangledResponseException;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.RegistrationRequest;
import com.google.dataconnector.util.RegistrationResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;

import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
  private static final Logger log = Logger.getLogger(ClientRegistrationUtil.class);
  
  private ClientRegistrationUtil() {} //Hide constructor for utility classes
  public static final String INITIAL_HANDSHAKE_MSG = "connect v1.0";
 
  /**
   * Authorizes this Secure Link connection from the info specified in the Client Configuration
   * object. 
   *  
   * @param socket The connected socket.
   * @returns the AuthRequest packet associated with this connection.
   * @throws AuthenticationException if authorization fails 
   * @throws MangledResponseException if an unexpected JSON encoding error occurs.
   * @throws IOException if socket communication errors occur.
   */
  public static AuthRequest authorize(final Socket socket, final ClientConf clientConf) 
      throws AuthenticationException, MangledResponseException, IOException {

    String userEmail = clientConf.getUser() + "@" + clientConf.getDomain();
    log.info("Attempting login for " + userEmail);

    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    try {
      // send a message to initiate handshake with tunnelserver
      log.info("Sending initial handshake msg");
      pw.println(INITIAL_HANDSHAKE_MSG);
      pw.flush();
      
      // create oauth aignature + request string
      long currentTime = System.currentTimeMillis() / 1000l;
      OAuthConsumer consumer = new OAuthConsumer(null, clientConf.getDomain(), 
          clientConf.getOauthKey(), null);
      OAuthMessage message = new OAuthMessage("GET", AuthRequest.URL_FOR_OAUTH, 
          new ArrayList<OAuth.Parameter>());
      message.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, AuthRequest.OAUTH_SIGNATURE_METHOD);
      message.addParameter(OAuth.OAUTH_VERSION, OAuth.VERSION_1_0);
      message.addParameter(OAuth.OAUTH_CONSUMER_KEY, consumer.consumerKey);
      message.addParameter(OAuth.OAUTH_TIMESTAMP, Long.toString(
          System.currentTimeMillis() / 1000l));
      message.addParameter(OAuth.OAUTH_NONCE, "doesnotmatter");
      message.addParameter(AuthRequest.OAUTH_REQUESTOR_ID_KEY, userEmail);
      OAuthAccessor accessor = new OAuthAccessor(consumer);
      message.sign(accessor);
      
      // create auth request packet
      AuthRequest authRequest = new AuthRequest();
      authRequest.setOauthString(AuthRequest.URL_FOR_OAUTH + "?" +
          OAuth.formEncode(message.getParameters()) + 
          "&" + OAuth.OAUTH_SIGNATURE+"=" + message.getSignature());
      
      // send auth request packet
      log.info("Sending login packet: " + authRequest.toJson().toString());
      pw.println(authRequest.toJson().toString());
      pw.flush();
      
      // wait for response and check.
      log.debug("Reading auth response");
      String jsonResponseString = br.readLine();
      log.debug("Got an auth response");
      
      AuthResponse authResponse = new AuthResponse(new JSONObject(jsonResponseString));
      if(authResponse.getStatus() != AuthResponse.Status.OK) {
        throw new AuthenticationException("Authentication Failed for " + clientConf.getEmail() + 
            ": " + authResponse.getStatus());
      }
      log.info("Login for " + clientConf.getEmail() + " successful");
      return authRequest;
    } catch (JSONException e) {
      throw new MangledResponseException("Mangled JSON response during auth: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new AuthenticationException("Authentication Failed due to Oauth error: " + 
          e.getMessage());
    } catch (OAuthException e) {
      throw new AuthenticationException("Authentication Failed due to Oauth error: " + 
          e.getMessage());
    }
  }
  
  /**
   * Send registration request which includes connection information (secure key and connection
   * rules).  When the server receives this request it registers the connection info with for
   * the domain this client connection is bound to.  
   * 
   * @param socket the connected socket.
   * @param authRequest the auth request for this connection.
   * @param clientConf the secure link global configuration object 
   * @throws RegistrationException if the registration fails.  This can happen if the server has 
   *             backend issues.
   * @throws MangledResponseException if an unexpected JSON encoding error occurs.
   * @throws IOException if any socket communication issues occur.
   */
  public static void register(final Socket socket, final AuthRequest authRequest, 
      final ClientConf clientConf) throws RegistrationException, MangledResponseException, 
      IOException {

    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader br = new BufferedReader(
        new InputStreamReader(socket.getInputStream()));

    try {
      // send registration request packet
      RegistrationRequest regRequest = new RegistrationRequest(clientConf);
      log.info("Registering rules: " + regRequest.toJson().toString());
      pw.println(regRequest.toJson().toString());
      pw.flush();
    
      // Receive registration response packet
      log.debug("Waiting for registration response");
      String jsonResponseString = br.readLine();
      log.debug("Read registration response: " + jsonResponseString);

      RegistrationResponse regResponse = new RegistrationResponse(
          new JSONObject(jsonResponseString));

      if (regResponse.getStatus() != RegistrationResponse.Status.OK) {
        throw new RegistrationException("Registration Failed: " + regResponse.getStatus());
      }
      log.info("Registration for " + clientConf.getEmail() + " successful");
    } catch (JSONException e) {
      throw new MangledResponseException("Mangled JSON response during registration: " + 
          e.getMessage());
    }
  }
}
