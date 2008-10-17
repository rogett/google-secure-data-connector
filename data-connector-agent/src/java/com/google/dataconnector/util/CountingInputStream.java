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

import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a normal input stream but takes an array of Counters to increment for every
 * byte read from the underyling InputStream.  This is useful for a varz counting traffic when 
 * dealing with third party libraries that support Socket replacement with a custom socket factory.
 * 
 * @see InputStream
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class CountingInputStream extends InputStream {

  private Counter[] counters; // list of normal counters
  private InputStream in; // underlying InputStream
 
  /**
   * Creates the CountingInputStream backed by the passed in counters. 
   * 
   * @param underlyingInputStream the input stream backing this CountingInputStream
   * @param counters the counters to increment.
   */
  public CountingInputStream(InputStream underlyingInputStream, Counter[] counters) { 
    this.in = underlyingInputStream;
    this.counters = counters;
  }
  
  /**
   * Loops through each of the counters and adds the specified amount.
   * 
   * @param amount the amount to add.
   */
  private void incrementCounters(int amount) {
    for (Counter counter : counters) {
      counter.increment(amount);
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int read(byte[] b) throws IOException {
    int numBytesRead = in.read(b); 
    if (numBytesRead > 0) {
        incrementCounters(numBytesRead);
    }
    return numBytesRead;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int read() throws IOException {
    int value = in.read();
    if (value > 0) {
      incrementCounters(1); // We don't add to our count unless the read actually succeeded.
    }
    return value;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int numBytesRead = in.read(b, off, len); 
    if (numBytesRead > 0) {
      incrementCounters(numBytesRead);
    }
    return numBytesRead;
  }
}
