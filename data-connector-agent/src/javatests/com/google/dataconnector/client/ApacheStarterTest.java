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
