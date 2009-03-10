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
package com.google.dataconnector.registration.v2;

/**
 * Exception for errors during protocol communications with Secure Link Server.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceException extends Exception {

  /**
   * Creates the exception with the specified error message.
   * 
   * @param msg the error message.
   */
  public ResourceException(String msg) {
    super(msg);
  }
  
  public ResourceException(Throwable cause) {
    super(cause);
  }
  
  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
