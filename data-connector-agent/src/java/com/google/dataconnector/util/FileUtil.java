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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Contains mockable convenience methods used for file manipulation.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class FileUtil {
  
  /**
   * Writes the provided string to a file.  This method will overwrite the file if it already
   * exists.
   * 
   * @param filename the filename to open.
   * @param contents the contents to place in the file.
   * @throws IOException if any file operations result in errors.
   */
  public void writeFile(String filename, String contents) throws IOException {
    
    // Delete any existing file.  Needed for win32 systems where renameTo breaks.
    // See java bug 4017593. http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4017593
    deleteFile(filename);
    
    // Write out tempfile.
    String tempFilename = filename + "-" + System.currentTimeMillis() + "-" + 
        new Random().nextInt();
    FileWriter fileWriter = new FileWriter(new File(tempFilename));
    fileWriter.write(contents);
    fileWriter.close();
    
    // Rename temp file to real file.  Atomic on UNIX.
    File tempfile = new File(tempFilename);
    tempfile.renameTo(new File(filename));
  }
  
  /**
   * Reads the file into a string. 
   * 
   * @param filename the filename to read.
   * @return a string with the file's contents.
   * @throws IOException if any file operations result in errors.
   */
  public String readFile(String filename) throws IOException {
    File file = new File(filename);
    FileReader fileReader = new FileReader(file);
    char[] cbuf = new char[(int) file.length()];
    fileReader.read(cbuf);
    fileReader.close();
    return new String(cbuf);
  }

  /**
   * Deletes a file.
   * 
   * @param filename
   */
  public void deleteFile(String filename) {
    File file = new File(filename); 
    file.delete();
  }
  
  /**
   * Sets a file for deletion on VM exit.
   * 
   * @param filename
   */
  public void deleteFileOnExit(String filename) {
    File file = new File(filename); 
    file.deleteOnExit();
  }
  
  /**
   * returns a FileInputStream
   * @param filename
   * @return FileInputStream for the given file
   * @throws FileNotFoundException 
   */
  public FileInputStream getFileInputStream(String filename) throws FileNotFoundException {
    return new FileInputStream(filename);
  }
}
