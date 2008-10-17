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

/**
 * Represents a counter with increment and retreive value operations.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public interface Counter {
  
  /**
   * Adds the specified delta to the counter.
   * 
   * @param delta the amount to add to the counter.
   */
  public void increment(int delta);
  
  /**
   * Retrieves the value of the counter.
   * 
   * @return the value of the counter.
   */
  public long get();
  
  /**
   * Print out value
   * 
   * @return String representing the value.
   */
  public String toString();
  
}
