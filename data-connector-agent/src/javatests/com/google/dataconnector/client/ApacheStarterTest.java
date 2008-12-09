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
import com.google.dataconnector.client.testing.StringArrayMatcher;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.dataconnector.util.ApacheHelper;
import com.google.dataconnector.util.LocalConf;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tests for the {@link ApacheStarter} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ApacheStarterTest extends TestCase {
  
  FakeLocalConfGenerator fakeLocalConfGenerator = new FakeLocalConfGenerator();
  FakeResourceRuleConfig fakeResourceRuleConfig = new FakeResourceRuleConfig();
  LocalConf localConf;
  List<ResourceRule> resourceRules;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    localConf = fakeLocalConfGenerator.getFakeLocalConf();
  }
  
  public void testRun() throws InterruptedException, IOException {
    // setup
    Process mockProcess = EasyMock.createMock(Process.class);
    EasyMock.expect(mockProcess.waitFor()).andReturn(0).anyTimes();
    ByteArrayInputStream bis = new ByteArrayInputStream("".getBytes());
    EasyMock.expect(mockProcess.getErrorStream()).andReturn(bis).anyTimes();
    EasyMock.replay(mockProcess);
    
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(checkRuntimeArgs())).andReturn(mockProcess);
    EasyMock.replay(mockRuntime);
    
    // test
    ApacheStarter apacheStart = new ApacheStarter(localConf, null, mockRuntime);
    apacheStart.run();
    
    // verify
    EasyMock.verify(mockProcess);
    EasyMock.verify(mockRuntime);
  }
  
  /**
   * Creates argument matcher that compares our expected runtime command line array.
   * 
   * @return null
   */
  public String[] checkRuntimeArgs() {
    EasyMock.reportMatcher(new StringArrayMatcher(new String[] {
        localConf.getApacheRoot() + File.separator + "bin" + File.separator + "httpd",
        "-D", "FOREGROUND",
        "-f", ApacheHelper.getHttpdConfFileName(localConf) }));
    return null;
  }
}
