/* Copyright 2009 Google Inc.
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

import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.protocol.FrameReceiver;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.proto.SdcFrame.AuthorizationInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo.Type;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;
import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;

import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

/**
 * Tests for the {@link SdcConnection} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SdcConnectionTest extends TestCase {
  
  private static final String EXPECTED_CN = FakeLocalConfGenerator.SDC_SERVER_HOST;
  private Principal mockPrincipal;
  private X509Certificate mockCert;
  private SSLSession mockSession;
  private LocalConf fakeLocalConf;
  private FrameSender mockFrameSender;
  private FrameReceiver mockFrameReceiver;
  
  @Override
  protected void setUp() throws Exception {
    // TODO Auto-generated method stub
    super.setUp();
    fakeLocalConf =  new FakeLocalConfGenerator().getFakeLocalConf();
    
    mockFrameSender = EasyMock.createMock(FrameSender.class);
    mockFrameSender.sendFrame(matchFrameInfo(
        fakeLocalConf.getUser() + "@" + fakeLocalConf.getDomain(),
        fakeLocalConf.getPassword(),
        FrameInfo.Type.AUTHORIZATION));
    EasyMock.expectLastCall();
    EasyMock.replay(mockFrameSender);
    
  }
  @Override
  protected void tearDown() throws Exception {
    mockPrincipal = null;
    mockCert = null;
    mockSession = null;
    super.tearDown();
  }
  
  public void testAuthorizeSuccess() throws Exception {

    AuthorizationInfo authInfoResponse = AuthorizationInfo.newBuilder()
        .setResult(AuthorizationInfo.ResultCode.OK)
        .build();
    FrameInfo frameInfoResponse = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.AUTHORIZATION)
        .setPayload(authInfoResponse.toByteString())
        .build();
    
    mockFrameReceiver = EasyMock.createMock(FrameReceiver.class);
    mockFrameReceiver.readOneFrame();
    EasyMock.expectLastCall().andReturn(frameInfoResponse);
    EasyMock.replay(mockFrameReceiver);
    
    SdcConnection sdcConnection = new SdcConnection(fakeLocalConf, null, mockFrameReceiver, 
        mockFrameSender, null ,null, null, null);
    
    assertTrue(sdcConnection.authorize());
    EasyMock.verify(mockFrameReceiver, mockFrameSender);
  }
  
  public void testAuthorizeFail() throws Exception {

    AuthorizationInfo authInfoResponse = AuthorizationInfo.newBuilder()
        .setResult(AuthorizationInfo.ResultCode.ACCESS_DENIED)
        .build();
    FrameInfo frameInfoResponse = FrameInfo.newBuilder()
        .setType(FrameInfo.Type.AUTHORIZATION)
        .setPayload(authInfoResponse.toByteString())
        .build();
    
    mockFrameReceiver = EasyMock.createMock(FrameReceiver.class);
    mockFrameReceiver.readOneFrame();
    EasyMock.expectLastCall().andReturn(frameInfoResponse);
    EasyMock.replay(mockFrameReceiver);
    
    SdcConnection sdcConnection = new SdcConnection(fakeLocalConf, null, mockFrameReceiver, 
        mockFrameSender, null ,null, null, null);
    
    assertFalse(sdcConnection.authorize());
    EasyMock.verify(mockFrameReceiver, mockFrameSender);
  }
  
  private FrameInfo matchFrameInfo(String expectedEmail, String expectedPassword, 
      Type expectedType) {
    EasyMock.reportMatcher(new AuthFrameInfoMatcher(expectedEmail, expectedPassword, 
        expectedType));
    return null;
  }
  
  private static class AuthFrameInfoMatcher implements IArgumentMatcher {
    
    private String expectedEmail;
    private String expectedPassword;
    private Type expectedType;

    public AuthFrameInfoMatcher(String email, String password, FrameInfo.Type type) {
      this.expectedEmail = email;
      this.expectedPassword = password;
      this.expectedType = type;
    }
    
    @Override
    public void appendTo(StringBuffer buffer) {
      buffer.append("frameInfoWith(" + expectedEmail + "," + expectedPassword + "," + 
          expectedType + ")"); 
    }
    
    @Override
    public boolean matches(Object actual) {
      if (!(actual instanceof FrameInfo)) {
        return false;  
      }
      FrameInfo actualFrameInfo = (FrameInfo) actual;
      if (expectedType != actualFrameInfo.getType()) {
        return false;
      }
      try {
        AuthorizationInfo actualAuthInfo = AuthorizationInfo.parseFrom(
            actualFrameInfo.getPayload());
        if (!actualAuthInfo.getEmail().equals(expectedEmail)) {
          return false;
        } else if (!actualAuthInfo.getPassword().equals(expectedPassword)) {
          return false;
        }
        // Everything matches.
        return true;
      } catch (InvalidProtocolBufferException e) {
        return false;
      }
    } 
  }
  
  /**
   * Configures mocks to return the supplied RFC2253 formatted DN.
   * @throws SSLPeerUnverifiedException 
   */
  private void createMockSession(LdapName expectedLdapName) throws SSLPeerUnverifiedException {
    mockPrincipal = EasyMock.createMock(Principal.class);
    mockPrincipal.getName();
    EasyMock.expectLastCall().andReturn(expectedLdapName.toString());
    EasyMock.replay(mockPrincipal);
    
    mockCert = EasyMock.createMock(X509Certificate.class);
    mockCert.getSubjectDN();
    EasyMock.expectLastCall().andReturn(mockPrincipal);
    EasyMock.replay(mockCert);
    
    mockSession = EasyMock.createMock(SSLSession.class);
    mockSession.getPeerCertificateChain();
    EasyMock.expectLastCall().andReturn(new X509Certificate[] { mockCert });
    EasyMock.replay(mockSession);
    
  }
  
  public void testVerifySubjectInCertificateGoodCn() throws InvalidNameException, 
      SSLPeerUnverifiedException, ConnectionException {
    
    // Setup
    createMockSession(new LdapName("CN=\"" + EXPECTED_CN + "\",OU=\"foobar\",C=\"bar\""));
    
    // Execute
    SdcConnection sdc = new SdcConnection(fakeLocalConf, null, null, null, null, null, null, null);
    sdc.verifySubjectInCertificate(mockSession);
    
    // Verify
    EasyMock.verify(mockSession);
    EasyMock.verify(mockCert);
  }
  
  public void testVerifySubjectInCertificateBadCn() throws InvalidNameException, 
      SSLPeerUnverifiedException {
    
    // Setup
    createMockSession(new LdapName("CN=\"" + "BADNESS" + "\",OU=\"foobar\",C=\"bar\""));
    
    // Execute
    SdcConnection sdc = new SdcConnection(fakeLocalConf, null, null, null, null, null, null, null);
    try {
      sdc.verifySubjectInCertificate(mockSession);
    } catch (ConnectionException e) {
      assertTrue(e.getMessage().startsWith("Wrong server"));
      EasyMock.verify(mockSession);
      EasyMock.verify(mockCert);
      return;
    }
    fail("Did not recieve ConnectionException");
  }
  
  public void testVerifySubjectInCertificateNoPeerChain() throws SSLPeerUnverifiedException {
    mockSession = EasyMock.createMock(SSLSession.class);
    mockSession.getPeerCertificateChain();
    EasyMock.expectLastCall().andThrow(new SSLPeerUnverifiedException("Fail"));
    EasyMock.replay(mockSession);
    
    SdcConnection sdc = new SdcConnection(null, null, null, null, null, null, null, null);
    try {
      sdc.verifySubjectInCertificate(mockSession);
    } catch (ConnectionException e) {
      assertTrue(e.getCause() instanceof SSLPeerUnverifiedException);
      EasyMock.verify(mockSession);
      return;
    }
  }
}
