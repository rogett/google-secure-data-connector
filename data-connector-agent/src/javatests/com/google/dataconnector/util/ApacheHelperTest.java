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
package com.google.dataconnector.util;

import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.dataconnector.util.ApacheHelper.ApacheVersion;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;

import javax.net.SocketFactory;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Tests for the {@link ApacheHelper} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheHelperTest extends TestCase {
  
  private final String APACHE_22_VERSION_OUTPUT = new String(
      "Server version: Apache/2.2.6 (Unix)\n" +
      "Server built:   Dec 17 2007 19:06:00\n");
  
  private final String APACHE_20_VERSION_OUTPUT = new String(
      "Server version: Apache/2.0.63 (Unix)\n" +
      "Server built:   Dec 17 2007 19:06:00\n");
  
  FakeLocalConfGenerator fakeLocalConfGenerator = new FakeLocalConfGenerator();
  FakeResourceRuleConfig fakeResourceRuleConfig = new FakeResourceRuleConfig();
  LocalConf localConf;
  List<ResourceRule> resourceRules;
  ApacheHelper apacheHelper;
  SocketFactory mockSocketFactory;
  Socket mockSocket;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    SocketAddress mockSocketAddress = EasyMock.createMock(SocketAddress.class);
    EasyMock.replay(mockSocketAddress);
    
    // Socket
    mockSocket = EasyMock.createMock(Socket.class);
    mockSocket.bind(EasyMock.isA(SocketAddress.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(mockSocket.getLocalPort()).andReturn(new Random().nextInt()).anyTimes();
    EasyMock.replay(mockSocket);
    
    // SocketFactory
    mockSocketFactory = EasyMock.createMock(SocketFactory.class);
    EasyMock.expect(mockSocketFactory.createSocket()).andReturn(mockSocket).anyTimes();
    EasyMock.replay(mockSocketFactory);
    
    localConf = fakeLocalConfGenerator.getFakeLocalConf(); 
    resourceRules = fakeResourceRuleConfig.getFakeRuntimeResourceRules();
    ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil(), 
        mockSocketFactory, mockSocketAddress);
  }
  
  @Override
  protected void tearDown() throws Exception {
    localConf = null;
    resourceRules = null;
    super.tearDown();
  }
  
  public void testGetApache22Version() throws ApacheSetupException, IOException {
    // Setup
    ByteArrayInputStream bis = new ByteArrayInputStream(APACHE_22_VERSION_OUTPUT.getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec((String[]) EasyMock.anyObject())).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    // Test
    ApacheHelper apacheHelper = new ApacheHelper(localConf, null, mockRuntime, null);
    ApacheVersion version = apacheHelper.getApacheVersion();
    
    // Verify
    assertEquals(ApacheVersion.TWOTWO, version);
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockProcess);
    EasyMock.verify(mockSocket);
    EasyMock.verify(mockSocketFactory);
  }
    
  public void testGetApache20Version() throws ApacheSetupException, IOException {
    // Setup
    ByteArrayInputStream bis = new ByteArrayInputStream(APACHE_20_VERSION_OUTPUT.getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class))).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    // Test
    ApacheHelper apacheHelper = new ApacheHelper(localConf, null, mockRuntime, null);
    ApacheVersion version = apacheHelper.getApacheVersion();
    
    // Verify
    assertEquals(ApacheVersion.TWOZERO, version);
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockProcess);
  }
  
  public void testGetApacheInvalidVersion() throws ApacheSetupException, IOException {
    // Setup
    ByteArrayInputStream bis = new ByteArrayInputStream("TOTALLY BOGUS VERSION".getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class))).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    // Test
    ApacheHelper apacheHelper = new ApacheHelper(localConf, null, mockRuntime, null);
    ApacheVersion version = apacheHelper.getApacheVersion();
    
    // Verify
    assertEquals(ApacheVersion.INVALID, version);
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockProcess);
  }
  
  public void testGetApacheVersionThrowsException() throws IOException {
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
      EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class)))
          .andThrow(new IOException("httpd execution error"));
    EasyMock.replay(mockRuntime);
    ApacheHelper apacheHelper = new ApacheHelper(localConf, null, mockRuntime, null);
    try {
      apacheHelper.getApacheVersion();
    } catch (ApacheSetupException e) {
      EasyMock.verify(mockRuntime);
      return;
    }
    fail("Did not recieve ApacheSetupException");
    
  }
    
  public void testGetHttpdConfFileName() throws Exception {
    apacheHelper = new ApacheHelper(null, null, null, null);
    assertEquals(FakeLocalConfGenerator.APACHE_CONF_DIR + File.separator + 
            ApacheHelper.HTTP_CONF_FILE_NAME + "." + 
            URLEncoder.encode(localConf.getClientId(), "UTF8"), 
        ApacheHelper.getHttpdConfFileName(localConf));
  }
  
  public void testGenerate22HttpdConf() throws IOException, ApacheSetupException {
    
    // We ignore the real conf and only put in the values that ApacheHelper should substitute.
    String testConf = "_PROXYMATCHRULES_\n" +
        "_PID_FILE_\n" +
        "_LISTENENTRIES_\n";
    
    // Setup test.
    ByteArrayInputStream bis = new ByteArrayInputStream(APACHE_22_VERSION_OUTPUT.getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class))).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    FileUtil mockFileUtil = EasyMock.createMock(FileUtil.class);
    EasyMock.expect(mockFileUtil.readFile(ApacheHelper.getHttpdConfTemplateFileName(localConf)))
        .andReturn(testConf);
    mockFileUtil.writeFile(EasyMock.eq(ApacheHelper.getHttpdConfFileName(localConf)), 
        checkHttpConf(ApacheVersion.TWOTWO));
    EasyMock.expectLastCall();
    mockFileUtil.deleteFile(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    mockFileUtil.deleteFileOnExit(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    EasyMock.replay(mockFileUtil);
    
    // Execute
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, mockRuntime, 
        mockFileUtil);
    apacheHelper.generateHttpdConf();
    
    // Check to see if our httpdconf has the right stuff in it.
    EasyMock.verify(mockFileUtil);
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockProcess);
    EasyMock.verify(mockSocketFactory);
    EasyMock.verify(mockSocket);
  }
  
  public void testGenerate20HttpdConf() throws IOException, ApacheSetupException {
    
    // We ignore the real conf and only put in the values that ApacheHelper should substitute.
    String testConf = "_PROXYMATCHRULES_\n" +
        "_PID_FILE_\n" +
        "_LISTENENTRIES_\n";
    
    // Setup test.
    ByteArrayInputStream bis = new ByteArrayInputStream(APACHE_20_VERSION_OUTPUT.getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class))).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    FileUtil mockFileUtil = EasyMock.createMock(FileUtil.class);
    EasyMock.expect(mockFileUtil.readFile(ApacheHelper.getHttpdConfTemplateFileName(localConf)))
        .andReturn(testConf);
    mockFileUtil.writeFile(EasyMock.eq(ApacheHelper.getHttpdConfFileName(localConf)), 
        checkHttpConf(ApacheVersion.TWOZERO));
    EasyMock.expectLastCall();
    mockFileUtil.deleteFile(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    mockFileUtil.deleteFileOnExit(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    EasyMock.replay(mockFileUtil);
    
    // Execute
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, mockRuntime, 
        mockFileUtil);
    apacheHelper.generateHttpdConf();
    
    // Check to see if our httpdconf has the right stuff in it.
    EasyMock.verify(mockFileUtil);
    EasyMock.verify(mockProcess);
    EasyMock.verify(mockRuntime);
  }
  
  public void testMakeProxyMatchConfEntries() throws ApacheSetupException, IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(APACHE_20_VERSION_OUTPUT.getBytes());
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.getInputStream()).andReturn(bis);
    mockProcess.destroy();
    EasyMock.expectLastCall();
    EasyMock.replay(mockProcess);
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(EasyMock.isA(String[].class))).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, mockRuntime, null);
    String proxyMatchEntries = apacheHelper.makeProxyMatchConfEntries();
    for (ResourceRule resourceRule : resourceRules) {
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        continue;
      }
      assertTrue(proxyMatchEntries.contains(resourceRule.getPattern()));
      assertTrue(proxyMatchEntries.contains(resourceRule.getHttpProxyPort().toString()));
    }
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockProcess);
  }
 
  /**
   * Creates the EasyMock report matcher for use with checking the Http Conf.
   * 
   * @param apacheVersion the version of conf we should check validity for.
   */
  public String checkHttpConf(ApacheVersion apacheVersion) {
    EasyMock.reportMatcher(new HttpConfChecker(localConf, resourceRules));
    return null; 
  }
  
  /**
   * Custom argument matcher to verify proper HTTP conf is written out.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public static class HttpConfChecker implements IArgumentMatcher {
    
    private LocalConf localConf;
    private List<ResourceRule> resourceRules;
    
    /**
     * Creates argument matcher with test data that is used to verify http conf.
     * 
     * @param localConf the local agent configuration used in the test.
     * @param resourceRules the resource rules used in the test.
     */
    public HttpConfChecker(LocalConf localConf, List<ResourceRule> resourceRules) {
      this.localConf = localConf;
      this.resourceRules = resourceRules;
    }
    
    /**
     * Adds the error to the EasyMock reporting.
     */
    public void appendTo(StringBuffer errors) {
      errors.append("Expected values not present in httpdConf");
    }

    /**
     * Checks to see that the substitutions in the apache conf file were performed.
     */
    public boolean matches(Object arg0) {
      
      // We only allow strings.
      if (!(arg0 instanceof String)) {
        return false; 
      }
      
      String httpConf = (String) arg0;  
      
      // Check httpd.pid
      if (!httpConf.contains(localConf.getApacheConfDir() + File.separator +
          "httpd.pid-" + localConf.getClientId())) {
        return false;
      }
      
      // Verify each pattern is in the written httpd conf.
      for (ResourceRule resourceRule : resourceRules) { 
        if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
          // Check to see Listen pattern made it.
          if (!(httpConf.contains("Listen " + localConf.getHttpProxyBindHost() + ":" +
              resourceRule.getHttpProxyPort().toString()))) {
            return false;
          }
          // Check pattern
          if (!(httpConf.contains(resourceRule.getPattern()))) {
            return false; 
          }
        }
      }
      return true;
    }
  }
}
