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
