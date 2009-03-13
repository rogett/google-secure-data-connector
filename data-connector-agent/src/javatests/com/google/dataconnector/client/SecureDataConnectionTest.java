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

import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

/**
 * Tests for the {@link SecureDataConnection} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SecureDataConnectionTest extends TestCase {
  
  private static final String EXPECTED_CN = FakeLocalConfGenerator.SDC_SERVER_HOST;
  private Principal mockPrincipal;
  private X509Certificate mockCert;
  private SSLSession mockSession;
  private LocalConf fakeLocalConf;
  private JsocksStarter mockJsocksStarter;
  
  @Override
  protected void setUp() throws Exception {
    // TODO Auto-generated method stub
    super.setUp();
    fakeLocalConf =  new FakeLocalConfGenerator().getFakeLocalConf();
  }
  @Override
  protected void tearDown() throws Exception {
    mockPrincipal = null;
    mockCert = null;
    mockSession = null;
    super.tearDown();
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
    
    mockJsocksStarter = EasyMock.createMock(JsocksStarter.class);
    mockJsocksStarter.startJsocksProxy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockJsocksStarter);    
  }
  
  public void testVerifySubjectInCertificateGoodCn() throws InvalidNameException, 
      SSLPeerUnverifiedException, ConnectionException {
    
    // Setup
    createMockSession(new LdapName("CN=\"" + EXPECTED_CN + "\",OU=\"foobar\",C=\"bar\""));
    
    // Execute
    SecureDataConnection sdc = new SecureDataConnection(fakeLocalConf, null, null, null,
        mockJsocksStarter); 
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
    SecureDataConnection sdc = new SecureDataConnection(fakeLocalConf, null, null, null,
        mockJsocksStarter); 
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
    
    SecureDataConnection sdc = new SecureDataConnection(null, null, null, null,
        mockJsocksStarter); 
    try {
      sdc.verifySubjectInCertificate(mockSession);
    } catch (ConnectionException e) {
      assertTrue(e.getCause() instanceof SSLPeerUnverifiedException);
      EasyMock.verify(mockSession);
      return;
    }
  }
}
