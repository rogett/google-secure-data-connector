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
 *
 * $Id$
 */
package com.google.dataconnector.util;

import com.google.dataconnector.client.testing.FakeLocalConfGenerator;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.File;

/**
 * Tests for the {@link LocalConfValidator} class.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class LocalConfValidatorTest extends TestCase {

  private LocalConf localConf;
  private LocalConfValidator localConfValidator;
  private FileUtil mockFileUtil;
  private File mockGoodFile;
  private File mockBadFile;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FakeLocalConfGenerator fakeLocalConfGenerator = new FakeLocalConfGenerator();
    localConf = fakeLocalConfGenerator.getFakeLocalConf();

    mockFileUtil = EasyMock.createMock(FileUtil.class);
    localConfValidator = new LocalConfValidator(mockFileUtil);

    // Good file.
    mockGoodFile = EasyMock.createMock(File.class);
    EasyMock.expect(mockGoodFile.canRead()).andReturn(true).anyTimes();
    EasyMock.replay(mockGoodFile);

    // Bad file should only occur once
    mockBadFile = EasyMock.createMock(File.class);
    EasyMock.expect(mockBadFile.canRead()).andReturn(false);
    EasyMock.replay(mockBadFile);

    // Successful filefactory
    EasyMock.expect(mockFileUtil.openFile(FakeLocalConfGenerator.RULES_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileUtil.openFile(FakeLocalConfGenerator.SSL_KEY_STORE_FILE)).andReturn(
        mockGoodFile);
    EasyMock.replay(mockFileUtil);
  }

  @Override
  protected void tearDown() throws Exception {
    localConf = null;
    localConfValidator = null;
    mockBadFile = null;
    mockGoodFile = null;
    mockFileUtil = null;
    super.tearDown();
  }

  public void testProperConfigResourceRules() throws LocalConfException {
    // Test successful base case.
    localConfValidator.validate(localConf);
    EasyMock.verify(mockGoodFile);
    EasyMock.verify(mockFileUtil);
  }

  // RulesFile
  public void testBadRulesFile() {
    // Setup bad data
    String badFile = "/bad/file";
    localConf.setRulesFile(badFile);

    // Create filefactory to give the right bad files.
    mockFileUtil = EasyMock.createMock(FileUtil.class);
    EasyMock.expect(mockFileUtil.openFile(badFile)).andReturn(mockBadFile);
    EasyMock.expect(mockFileUtil.openFile(FakeLocalConfGenerator.SSL_KEY_STORE_FILE)).andReturn(
        mockGoodFile);
    EasyMock.replay(mockFileUtil);

    // Create new validator with our updated factory.
    localConfValidator = new LocalConfValidator(mockFileUtil);

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("Cannot read"));
      EasyMock.verify(mockBadFile);
      EasyMock.verify(mockGoodFile);
      EasyMock.verify(mockFileUtil);
      return;
    }
    fail("did not get LocalConfException");
  }

  // Socks Server Port
  public void testBadSdcServerPort() {
    // Setup bad data
    localConf.setSdcServerPort(3242343);

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("invalid 'sdcServerPort'"));
      return;
    }
    fail("did not get LocalConf");
  }

  // Domain
  public void testBadDomain() {
    // Setup bad data
    localConf.setDomain("asdfasdf");

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("not valid"));
      return;
    }
    fail("did not get LocalConf");
  }

  public void testBadDomainInvalidTld() {
    // Setup bad data
    localConf.setDomain("asdfasdf.asdfasfdafdsfadsf");

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("not valid"));
      return;
    }
    fail("did not get LocalConf");
  }

  // SslKeyStoreFile
  public void testBadSslKeystoreFile() {
    // Setup bad data
    String badFile = "/bad/file";
    localConf.setSslKeyStoreFile(badFile);

    // Create filefactory to give the right bad files.
    mockFileUtil = EasyMock.createMock(FileUtil.class);
    EasyMock.expect(mockFileUtil.openFile(FakeLocalConfGenerator.RULES_FILE)).andReturn(
        mockGoodFile);
    EasyMock.expect(mockFileUtil.openFile(badFile)).andReturn(mockBadFile);
    EasyMock.replay(mockFileUtil);

    // Create new validator with our updated factory.
    localConfValidator = new LocalConfValidator(mockFileUtil);

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("Cannot read"));
      EasyMock.verify(mockBadFile);
      EasyMock.verify(mockGoodFile);
      EasyMock.verify(mockFileUtil);
      return;
    }
    fail("did not get LocalConfException");
  }

  // Socks Server Port
  public void testBadSocksServerPort() {
    // Setup bad data
    localConf.setSocksServerPort(3242343);

    // Test and verify
    try {
      localConfValidator.validate(localConf);
    } catch (LocalConfException e) {
      assertTrue(e.getMessage().contains("invalid 'socks"));
      return;
    }
    fail("did not get LocalConf");
  }
}
