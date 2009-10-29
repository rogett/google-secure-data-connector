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
 *
 * $Id$
 */
package com.google.dataconnector.client;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.ShutdownManager;
import com.google.dataconnector.util.Stoppable;
import com.google.dataconnector.util.SystemUtil;

/**
 * Tests for the {@link ResourcesFileWatcher} class.
 *
 * @author mtp@google.com (Matt T. Proud)
 */
public class ResourcesFileWatcherTest extends TestCase {
  private static final String TEST_FILE = "test_rules_file";

  private LocalConf localConf;
  private Registration registration;
  private FrameSender frameSender;
  private FileUtil fileUtil;
  private SystemUtil systemUtil;
  private File fileHandle;
  private FileInputStream fileInputStream;
  private ShutdownManager shutdownManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    localConf = new LocalConf();
    localConf.setRulesFile(TEST_FILE);

    frameSender = createMock(FrameSender.class);
    fileHandle = EasyMock.createMock(File.class);
    systemUtil = EasyMock.createMock(SystemUtil.class);
    registration = EasyMock.createMock(Registration.class);
    fileUtil = EasyMock.createMock(FileUtil.class);
    fileInputStream = EasyMock.createMock(FileInputStream.class);
    shutdownManager = EasyMock.createMock(ShutdownManager.class);
    shutdownManager.addStoppable(isA(Stoppable.class));
    expectLastCall();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    verifyAll();
  }

  /**
   * No change in content, so no new registration should result.
   */
  public void testRun_noChangeNoReRegistration() throws InterruptedException, IOException {
    // Expected operation:
    //   1st time - Read.
    //   2nd time - Read.
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(1);
    expect(fileInputStream.read()).andReturn(-1);
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(1);
    expect(fileInputStream.read()).andReturn(-1);
    // Exit loop with fake exception.
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());

    replayAll();

    // Test now.
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil, shutdownManager);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // Verify done in tearDown().
  }

  /**
   * The file is modified, so re-registration occurs.
   */
  public void testRun_changeRequiresReRegistration() throws InterruptedException,
      RegistrationException, IOException {
    // Expected operation:
    //   1st time - Read.
    //   2nd time - Read.
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(1);
    expect(fileInputStream.read()).andReturn(-1);
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(2);
    expect(fileInputStream.read()).andReturn(-1);
    // Exit loop with fake exception.
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());

    registration.sendRegistrationInfo(frameSender);

    replayAll();

    // Test now.
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil, shutdownManager);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // Verify done in tearDown().
  }

  /**
   * The file is modified, so re-registration occurs.
   */
  public void testRun_failedReRegistrationRetries() throws InterruptedException,
      RegistrationException, IOException {
    // Expected operation:
    //   1st time - Read.
    //   2nd time - Read.
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(1);
    expect(fileInputStream.read()).andReturn(-1);
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(2);
    expect(fileInputStream.read()).andReturn(-1);
    expect(fileUtil.getFileInputStream(TEST_FILE)).andReturn(fileInputStream);
    expect(fileInputStream.read()).andReturn(2);
    expect(fileInputStream.read()).andReturn(-1);

    registration.sendRegistrationInfo(frameSender);
    expectLastCall().andThrow(new RegistrationException(""));
    registration.sendRegistrationInfo(frameSender);

    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    // Exit loop with fake exception.
    systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
    expectLastCall().andThrow(new InterruptedException());

    replayAll();

    // Test now.
    ResourcesFileWatcher watcher = new ResourcesFileWatcher(localConf, registration,
        fileUtil, systemUtil, shutdownManager);
    watcher.setFrameSender(frameSender);
    watcher.run();

    // Verify done in tearDown().
  }

  private void replayAll() {
    replay(frameSender);
    replay(fileUtil);
    replay(registration);
    replay(fileHandle);
    replay(systemUtil);
    replay(fileInputStream);
    replay(shutdownManager);
  }

  private void verifyAll() {
    verify(frameSender);
    verify(fileUtil);
    verify(registration);
    verify(fileHandle);
    verify(systemUtil);
    verify(fileInputStream);
    verify(shutdownManager);
  }
}
