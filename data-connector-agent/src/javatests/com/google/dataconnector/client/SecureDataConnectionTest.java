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
    
  }
  
  public void testVerifySubjectInCertificateGoodCn() throws InvalidNameException, 
      SSLPeerUnverifiedException, ConnectionException {
    
    // Setup
    createMockSession(new LdapName("CN=\"" + EXPECTED_CN + "\",OU=\"foobar\",C=\"bar\""));
    
    // Execute
    SecureDataConnection sdc = new SecureDataConnection(fakeLocalConf, null, null, null); 
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
    SecureDataConnection sdc = new SecureDataConnection(fakeLocalConf, null, null, null); 
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
    
    SecureDataConnection sdc = new SecureDataConnection(null, null, null, null); 
    try {
      sdc.verifySubjectInCertificate(mockSession);
    } catch (ConnectionException e) {
      assertTrue(e.getCause() instanceof SSLPeerUnverifiedException);
      EasyMock.verify(mockSession);
      return;
    }
  }
}
