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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.SystemUtil;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.File;

/**
 * Tests for the {@link ResourcesFileWatcher} class.
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class ResourcesFileWatcherTest extends TestCase {


  private static final String TEST_FILE = "test_rules_file";
  private LocalConf localConf = new LocalConf();
  private Registration registration;
  private FrameSender frameSender;
  private FileUtil fileUtil;
  private SystemUtil systemUtil;
  private File fileHandle;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    localConf.setRulesFile(TEST_FILE);

    frameSender = createMock(FrameSender.class);
    replay(frameSender);

    fileHandle = EasyMock.createMock(File.class);
    systemUtil = EasyMock.createMock(SystemUtil.class);
    registration = EasyMock.createMock(Registration.class);

    fileUtil = EasyMock.createMock(FileUtil.class);
    expect(fileUtil.openFile(TEST_FILE)).andReturn(fileHandle);
    replay(fileUtil);
  }

  @Override
  protected void tearDown() throws Exception {
    verify(frameSender);
    verify(fileUtil);
    verify(registration);
    verify(fileHandle);
    verify(systemUtil);

    super.tearDown();
  }

  /**
   * no change in file timestamp - so, registration should not be called.
   */
  public void testNoChangeInFileTimestamp() throws InterruptedException {
    // no method on this should be called
    replay(registration);

    // file timestamp never changes
    expect(fileHandle.lastModified()).andReturn(1L).anyTimes();
    replay(fileHandle);

    // sleep behavior
    //   1st time - no exception thrown
    //   2nd time - exception thrown
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall();
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());
    replay(systemUtil);

    // test now
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // verify done in tearDown()
  }

  /**
   * file timestamp change is noticed - so, registration should be called.
   */
  public void testFileChangeNoticedOnce() throws InterruptedException, RegistrationException {
    // file timestamp changes once and then remains the same after that
    expect(fileHandle.lastModified()).andReturn(1L);
    expect(fileHandle.lastModified()).andReturn(2L);
    expect(fileHandle.lastModified()).andReturn(2L).anyTimes();
    replay(fileHandle);

    // registration should occur once
    registration.sendRegistrationInfo(frameSender);
    expectLastCall();
    replay(registration);

    // sleep behavior
    //   1st time - no exception thrown
    //   2nd time - no exception thrown
    //   3rd time - exception thrown
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall();
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall();
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());
    replay(systemUtil);

    // test now
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // verify done in tearDown()
  }

  /**
   * file timestamp change is noticed N times - so, registration should be called N times.
   */
  public void testFileChangeNoticedNtimes() throws InterruptedException, RegistrationException {
    int n = 5; // file changes 5 times

    long changeTimestamp = 1L;
    expect(fileHandle.lastModified()).andReturn(changeTimestamp);
    for (int i = 0; i < n; i++, changeTimestamp++) {
      expect(fileHandle.lastModified()).andReturn(changeTimestamp);
    }
    // no more changes in timestamp
    expect(fileHandle.lastModified()).andReturn(changeTimestamp).anyTimes();
    replay(fileHandle);

    // registration should occur once
    registration.sendRegistrationInfo(frameSender);
    expectLastCall().times(n);
    replay(registration);

    // sleep behavior
    //   1 n times - no exception thrown
    //   (n+1) attempt - exception thrown
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().times(n);
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());
    replay(systemUtil);

    // test now
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // verify done in tearDown()
  }
}
