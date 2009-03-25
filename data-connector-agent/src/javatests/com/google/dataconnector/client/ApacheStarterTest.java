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

import com.google.dataconnector.client.ApacheStarter.ApacheCommand;
import com.google.dataconnector.client.testing.FakeLocalConfGenerator;
import com.google.dataconnector.client.testing.StringArrayMatcher;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.dataconnector.util.ApacheHelper;
import com.google.dataconnector.util.ApacheSetupException;
import com.google.dataconnector.util.LocalConf;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayInputStream;
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
  
  public void testStartApacheHttpd() throws IOException, ApacheSetupException {
    // setup
    ApacheHelper mockApacheHelper = EasyMock.createMock(ApacheHelper.class);
    mockApacheHelper.generateHttpdConf();
    EasyMock.expectLastCall();
    EasyMock.replay(mockApacheHelper);
    
    Process mockProcess = EasyMock.createMock(Process.class);
    ByteArrayInputStream bis = new ByteArrayInputStream("".getBytes());
    EasyMock.expect(mockProcess.getErrorStream()).andReturn(bis).anyTimes();
    EasyMock.replay(mockProcess);
    
    Runtime mockRuntime = EasyMock.createMock(Runtime.class);
    EasyMock.expect(mockRuntime.exec(checkRuntimeArgs(ApacheCommand.STOP))).andReturn(mockProcess);
    EasyMock.expectLastCall();
    EasyMock.expect(mockRuntime.exec(checkRuntimeArgs(ApacheCommand.START))).andReturn(mockProcess);
    EasyMock.expectLastCall();
    mockRuntime.addShutdownHook(EasyMock.isA(Thread.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockRuntime);
    
    // test
    ApacheStarter apacheStart = new ApacheStarter(localConf, mockApacheHelper, mockRuntime);
    apacheStart.startApacheHttpd();
    
    // verify
    EasyMock.verify(mockProcess);
    EasyMock.verify(mockApacheHelper);
    EasyMock.verify(mockRuntime);
  }
  
  /**
   * Creates argument matcher that compares our expected runtime command line array.
   * 
   * @return null
   */
  public String[] checkRuntimeArgs(ApacheCommand command) {
    EasyMock.reportMatcher(new StringArrayMatcher(new String[] {
        localConf.getApacheCtl(),
        "-f", ApacheHelper.getHttpdConfFileName(localConf),
        "-k", command.toString() }));
    return null;
  }
}
