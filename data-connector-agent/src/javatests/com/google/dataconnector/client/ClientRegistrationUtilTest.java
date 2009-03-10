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

import com.google.dataconnector.client.ClientRegistrationUtil;
import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.dataconnector.registration.v2.AuthResponse;
import com.google.dataconnector.registration.v2.RegistrationRequest;
import com.google.dataconnector.registration.v2.RegistrationResponse;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.dataconnector.util.AuthenticationException;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.RegistrationException;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Tests for the {@link ClientRegistrationUtil} class.
 * 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class ClientRegistrationUtilTest extends TestCase {

  private AuthRequest authRequest;
  private RegistrationRequest regRequest;
  private FakeLocalConfGenerator fakeLocalConfGenerator;
  private FakeResourceRuleConfig fakeResourceRuleConfig;
  private ClientRegistrationUtil clientRegistrationUtil;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    fakeLocalConfGenerator = new FakeLocalConfGenerator();
    fakeResourceRuleConfig = new FakeResourceRuleConfig();
    authRequest = new AuthRequest();
    ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil(), null, 
        null);
    regRequest = new RegistrationRequest(resourceRuleUtil);
    clientRegistrationUtil = new ClientRegistrationUtil(authRequest, regRequest);
  }

  /**
   * Creates mock socket from the input streams provided for use with the registration util.
   * 
   * @param is pre populated input stream such as a {@link ByteArrayInputStream}
   * @param os pre populated output stram such as a {@link ByteArrayOutputStream}
   * @return configured mock socket.
   * @throws IOException to satisfy Socket class.
   */
  private Socket getFakeSocket(InputStream is, OutputStream os) throws IOException {
    Socket fakeSocket = EasyMock.createMock(Socket.class);
    EasyMock.expect(fakeSocket.getInputStream()).andReturn(is);
    EasyMock.expect(fakeSocket.getOutputStream()).andReturn(os);
    EasyMock.replay(fakeSocket);
    return fakeSocket;
  }
  
  public void testAuthorize() throws IOException, JSONException {
    
    // Successful case.
    AuthResponse authResponse = new AuthResponse();
    authResponse.setStatus(AuthResponse.Status.OK);
    InputStream is = new ByteArrayInputStream(
        (authResponse.toJson().toString() + "\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    try {
      clientRegistrationUtil.authorize(getFakeSocket(is, os),
          fakeLocalConfGenerator.getFakeLocalConf());
    } catch (AuthenticationException e) {
      fail("not supposed to receive exception");
    }
    
    // Access Denied Case
    authResponse = new AuthResponse();
    authResponse.setStatus(AuthResponse.Status.ACCESS_DENIED);
    is = new ByteArrayInputStream((authResponse.toJson().toString() + "\n").getBytes());
    os = new ByteArrayOutputStream();
    boolean threwException = false;
    try {
      clientRegistrationUtil.authorize(getFakeSocket(is, os),
          fakeLocalConfGenerator.getFakeLocalConf());
    } catch (AuthenticationException e) {
      threwException = true;
    } 
    assertTrue(threwException);
    
    // Mangled Server Response.
    is = new ByteArrayInputStream("SO NOT A REAL JSON STRING\n".getBytes());
    os = new ByteArrayOutputStream();
    threwException = false;
    try {
      clientRegistrationUtil.authorize(getFakeSocket(is, os),
          fakeLocalConfGenerator.getFakeLocalConf());
    } catch (AuthenticationException e) {
      threwException = true;
      assertTrue(e.getMessage().startsWith("Mangled"));
    } 
    assertTrue(threwException);
  }
  
  public void testRegister() throws IOException, ConnectionException, JSONException {

    // Successful case. Will throw RegistrationException if it fails.
    RegistrationResponse responseJson = new RegistrationResponse();
    responseJson.setStatus(RegistrationResponse.Status.OK);
    InputStream is = new ByteArrayInputStream((responseJson.toJson().toString() + "\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    clientRegistrationUtil.register(getFakeSocket(is, os), authRequest, 
        fakeResourceRuleConfig.getFakeRuntimeResourceRules());

    // Negative Server Response case:
    responseJson = new RegistrationResponse();
    responseJson.setStatus(RegistrationResponse.Status.REGISTRATION_ERROR);
    responseJson.setErrorMsg("Some registration error");
    is = new ByteArrayInputStream((responseJson.toJson().toString() + "\n").getBytes());
    os = new ByteArrayOutputStream();
    boolean threwException = false;
    try {
      clientRegistrationUtil.register(getFakeSocket(is, os), authRequest,
          fakeResourceRuleConfig.getFakeRuntimeResourceRules());
    } catch (RegistrationException e) {
      threwException = true;
    } 
    assertTrue(threwException);

    // Mangled Server Response
    is = new ByteArrayInputStream("SO NOT A VALID JSON PACKET\n".getBytes());
    os = new ByteArrayOutputStream();
    threwException = false;
    try {
      clientRegistrationUtil.register(getFakeSocket(is, os), authRequest, 
          fakeResourceRuleConfig.getFakeRuntimeResourceRules());
    } catch (RegistrationException e) {
      threwException = true;
      assertTrue(e.getMessage().startsWith("Mangled"));
    } 
    assertTrue(threwException);
  }
}
