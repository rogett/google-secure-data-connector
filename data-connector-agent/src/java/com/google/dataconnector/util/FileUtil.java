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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
    
    deleteFile(filename);
    FileWriter fileWriter = new FileWriter(new File(filename));
    fileWriter.write(contents);
    fileWriter.close();
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
}
