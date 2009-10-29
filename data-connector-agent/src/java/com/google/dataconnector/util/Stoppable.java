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
package com.google.dataconnector.util;

/**
 * Classes should implement this interface if they need to be shutdown gracefully.  This may
 * include threads with while/true loops or classes that contain static state that needs
 * to be reset.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public interface Stoppable {
  
  /**
   * Shutdown gracefully.
   */
  public void shutdown();

}
