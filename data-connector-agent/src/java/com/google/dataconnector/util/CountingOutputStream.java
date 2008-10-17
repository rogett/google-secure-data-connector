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
import java.io.OutputStream;

/**
 * Wraps a normal output stream but takes an AtomicInteger which is passed by "reference" that
 * can be global for a given server. This is useful for a varz counting traffic when dealing 
 * with third party libraries that support Socket replacement with a custom socket factory.
 * 
 * We wrote our own because 
 * it extends {@link java.io.FilterOutputStream}  which maps 
 * {@link java.io.FilterOutputStream#write(byte[])} by looping through the byte array and
 * making individual 
 * {@link java.io.FilterOutputStream#write(int)} calls.  This is horribly inefficient.
 * 
 * @see OutputStream
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class CountingOutputStream extends OutputStream {

  private Counter[] counters; // list of normal counters
  private OutputStream out;
  
  /**
   * Creates the CountingOutputStream backed by the passed in OutputStream.  
   * 
   * @param underlyingOutputStream the output stream we are backed by.
   * @param counters the counters to increment.
   */
  public CountingOutputStream(OutputStream underlyingOutputStream, Counter[] counters) { 
    this.out = underlyingOutputStream;
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
  public void write(int b) throws IOException {
    out.write(b);
    incrementCounters(1);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void write(byte[] b) throws IOException {
    out.write(b);
    incrementCounters(b.length);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
    incrementCounters(len);
  }
}
