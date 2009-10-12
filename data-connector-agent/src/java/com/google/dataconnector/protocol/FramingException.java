/* Copyright 2009 Google Inc.
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
package com.google.dataconnector.protocol;

/**
 * Represents an error in the underlying framing protocol
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FramingException extends Exception {
  private static final long serialVersionUID = -4056983786604302186L;

  /**
   * Builds the exception with the specified message.
   *
   * @param msg the error message.
   */
  public FramingException(final String msg) {
    super(msg);
  }

  /**
   * Constructs the exception by setting the cause from provided throwable.
   *
   * @param cause the underlying cause of the exception.
   */
  public FramingException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs the exception with a message and cause.
   *
   * @param msg A string representing the error message.
   * @param cause the underlying cause of the exception.
   */
  public FramingException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}
