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

/**
 * Contains convenience methods for System calls to facilitate unittesting of the methods
 * that need these System calls
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class SystemUtil {

  /**
   * sleep for the given amount of time.
   *
   * @param sleepTime amount of time to sleep (in this thread)
   * @throws InterruptedException thrown if the thread is interrupted
   */
  public void sleep(long sleepTime) throws InterruptedException {
    Thread.sleep(sleepTime);
  }
}
