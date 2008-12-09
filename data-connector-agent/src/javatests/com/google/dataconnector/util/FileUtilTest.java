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
