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
package com.google.dataconnector.client.testing;

import org.easymock.IArgumentMatcher;

import java.util.Arrays;

/**
 * EasyMock matcher that compares two String arrays.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class StringArrayMatcher implements IArgumentMatcher {

  private String[] expected;

  /**
   * Creates the array matcher with the expected array.
   * @param expected
   */
  public StringArrayMatcher(String[] expected) {
    this.expected = expected;
  }

  /**
   * Adds the error to the EasyMock report.
   */
  public void appendTo(StringBuffer errors) {
    errors.append("Arguments not identical");
  }

  /**
   * Loops through each element of the expected array and ensures it exists in the actual array.
   */
  public boolean matches(Object actual) {
    if (!(actual instanceof String[])) {
      return false; 
    }
    return Arrays.equals(expected, (String[]) actual); 
  }
}