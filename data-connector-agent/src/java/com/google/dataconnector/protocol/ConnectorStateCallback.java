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
 */ 
package com.google.dataconnector.protocol;

/**
 * Interface that lets the {@link InputStreamConnector} and {@link OutputStreamConnector} 
 * notify its users that a connection state change has occurred.  Implement this to 
 * release resources if a given connection closes..
 * 
 * @author rayc@google.com (Ray Colline)
 */
public interface ConnectorStateCallback {
  
  /**
   * This is fired when the input or output streams are closed on the underlying streams.
   * 
   * @param connectionId the connection id that this connector is managing.
   */
  public void close(int connectionId);

}
