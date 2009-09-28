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

import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;

/**
 * Represents a handler that can parse the frameInfo to receive the payload.
 *
 * @author rayc@google.com (Ray Colline)
 *
 */
public interface Dispatchable {

  /**
   * Parse the frameInfo and process the given frame.
   *
   * @param frameInfo a frame.
   */
  public void dispatch(FrameInfo frameInfo) throws FramingException;

}
