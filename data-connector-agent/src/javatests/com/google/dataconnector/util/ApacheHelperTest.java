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
import com.google.dataconnector.client.testing.StringArrayMatcher;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Tests for the {@link ApacheHelper} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheHelperTest extends TestCase {
  
  FakeLocalConfGenerator fakeLocalConfGenerator = new FakeLocalConfGenerator();
  FakeResourceRuleConfig fakeResourceRuleConfig = new FakeResourceRuleConfig();
  LocalConf localConf;
  List<ResourceRule> resourceRules;
  ApacheHelper apacheHelper;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    localConf = fakeLocalConfGenerator.getFakeLocalConf(); 
    resourceRules = fakeResourceRuleConfig.getFakeRuntimeResourceRules();
    ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil();
    resourceRuleUtil.setHttpProxyPorts(resourceRules, localConf.getHttpProxyPort());
  }
  
  @Override
  protected void tearDown() throws Exception {
    localConf = null;
    resourceRules = null;
    super.tearDown();
  }

  public void testGetHttpdConfFileName() {
    apacheHelper = new ApacheHelper(null, null, null, null);
    assertEquals(FakeLocalConfGenerator.APACHE_CONF_DIR + File.separator + 
        ApacheHelper.HTTP_CONF_FILE_NAME, ApacheHelper.getHttpdConfFileName(localConf));
  }
  
  public void testGenerateHttpdConf() throws IOException, ApacheSetupException {
    
    String testConf = "_PROXYMATCHRULES_\n" +
        "_APACHE_PORT_\n" +
        "_APACHE_BIND_HOST_\n";
    
    // Setup test.
    FileUtil mockFileUtil = EasyMock.createMock(FileUtil.class);
    EasyMock.expect(mockFileUtil.readFile(ApacheHelper.getHttpdConfTemplateFileName(localConf)))
        .andReturn(testConf);
    mockFileUtil.writeFile(EasyMock.eq(ApacheHelper.getHttpdConfFileName(localConf)), 
        checkHttpConf());
    EasyMock.expectLastCall();
    mockFileUtil.deleteFile(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    mockFileUtil.deleteFileOnExit(ApacheHelper.getHttpdConfFileName(localConf));
    EasyMock.expectLastCall();
    EasyMock.replay(mockFileUtil);
    
    // Execute
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, null, mockFileUtil);
    apacheHelper.generateHttpdConf();
    
    // Check to see if our httpdconf has the right stuff in it.
    EasyMock.verify(mockFileUtil);
  }
  
  public void testMakeProxyMatchConfEntries() {
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, null, null);
    String proxyMatchEntries = apacheHelper.makeProxyMatchConfEntries();
    for (ResourceRule resourceRule : resourceRules) {
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        continue;
      }
      assertTrue(proxyMatchEntries.contains(resourceRule.getPattern()));
      assertTrue(proxyMatchEntries.contains(ApacheHelper.makeHtpasswdPath(localConf, 
          resourceRule.getName())));
    }
  }
 
  public void testGenerateHtpasswdFiles() throws ApacheSetupException, InterruptedException, 
      IOException {
    // Setup
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    FileUtil mockFileUtil = EasyMock.createMock(FileUtil.class);
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.waitFor()).andReturn(0).anyTimes();
    ByteArrayInputStream bis = new ByteArrayInputStream("".getBytes());
    EasyMock.expect(mockProcess.getErrorStream()).andReturn(bis).anyTimes();
    EasyMock.replay(mockProcess);
    
    for (ResourceRule resourceRule : resourceRules) {
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        continue;
      }
      
      // Mock Process
      
      // Mock File Util
      String expectedHtpasswdFile = ApacheHelper.makeHtpasswdPath(localConf, 
          resourceRule.getName());
      mockFileUtil.deleteFile(expectedHtpasswdFile);
      EasyMock.expectLastCall();
      mockFileUtil.deleteFileOnExit(expectedHtpasswdFile);
      EasyMock.expectLastCall();
      EasyMock.replay(mockFileUtil);
      
      // Mock Runtime
      EasyMock.expect(mockRuntime.exec(checkRuntimeArgs(resourceRule, expectedHtpasswdFile)))
         .andReturn(mockProcess);
      EasyMock.replay(mockRuntime);
    }
    
    // Test
    ApacheHelper apacheHelper = new ApacheHelper(localConf, resourceRules, mockRuntime, 
        mockFileUtil);
    apacheHelper.generateHtpasswdFiles();
    
    // Verify
    EasyMock.verify(mockRuntime);
    EasyMock.verify(mockFileUtil);
    EasyMock.verify(mockProcess);
  }
  
  /**
   * Creates argument matcher that compares our expected runtime command line array.
   * 
   * @param resourceRule expected ResourceRule
   * @param expectedHtpasswdFile expected htpasswd file name.
   * @return null
   */
  public String[] checkRuntimeArgs(ResourceRule resourceRule, String expectedHtpasswdFile) {
    EasyMock.reportMatcher(new StringArrayMatcher(new String[] {
          localConf.getApacheHtpasswd(),
          "-b",
          "-c", 
          expectedHtpasswdFile,
          ApacheHelper.DEFAULT_PROXY_AUTH_USER,
          resourceRule.getSecretKey().toString() }));
    return null;
  }
  
  /**
   * Creates the EasyMock report matcher for use with checking the Http Conf.
   */
  public String checkHttpConf() {
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
      // Verify the http proxy bind host was set.
      if (!httpConf.contains(localConf.getHttpProxyBindHost())) {
        return false;
      }
      
      // Verify each pattern is in the written httpd conf.
      for (ResourceRule resourceRule : resourceRules) { 
        if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID) && 
            (!(httpConf.contains(resourceRule.getHttpProxyPort().toString()))) && 
            (!(httpConf.contains(resourceRule.getPattern())))) {
          return false;
        }
      }
      return true;
    }
  }
}
