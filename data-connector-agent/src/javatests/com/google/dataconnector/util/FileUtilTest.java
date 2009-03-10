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
package com.google.dataconnector.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Tests for the {@link FileUtil} class.
 * 
 * This is a "LargeTest" as it writes files.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class FileUtilTest extends TestCase {
  
  private static String TEST_FILE_NAME = "/tmp/foo" + System.currentTimeMillis();
  private static String TEST_FILE = "wooot\nIm\na\nfile.\n";
  private static String TEST_NONEXISTANT_DIR = "/tmp/this/dir/should/not/exist/adsfs";
 
  private FileUtil fileUtil;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fileUtil = new FileUtil();
  }
  
  public void testWriteAndReadFileSuccess() throws IOException {
    fileUtil.writeFile(TEST_FILE_NAME, TEST_FILE);
    String writtenFile = fileUtil.readFile(TEST_FILE_NAME); 
    assertEquals(TEST_FILE, writtenFile);
  }
  
  public void testWriteFileFailure() {
    // Make sure it really doesnt exist.
    File file = new File(TEST_NONEXISTANT_DIR);
    assertFalse(file.exists());
    
    try {
      fileUtil.writeFile(TEST_NONEXISTANT_DIR, TEST_FILE);
    } catch (IOException e) {
      return;
    }
    fail("Did not throw IO Exception");
  }
  
  public void testReadFileFailure() {
    // Make sure it really doesnt exist.
    File file = new File(TEST_NONEXISTANT_DIR);
    assertFalse(file.exists());
    
    try {
      fileUtil.readFile(TEST_NONEXISTANT_DIR);
    } catch (IOException e) {
      return;
    }
    fail("Did not throw IO Exception");
  }
  
  public void testDeleteFile() throws IOException {
    fileUtil.writeFile(TEST_FILE_NAME, TEST_FILE);
    fileUtil.deleteFile(TEST_FILE_NAME);
    File file = new File(TEST_FILE_NAME);
    assertFalse(file.exists());
  }
  
}
