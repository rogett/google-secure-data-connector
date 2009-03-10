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

import junit.framework.TestCase;

/**
 * Tests for the {@link SocketInfo} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SocketInfoTest extends TestCase {

  private static final String SOCKET_PATTERN = "socket://128.195.131.10:3806";
  private static final String BAD_SOCKET_PATTERN_URLID = 
    "badness://128.195.131.10";
  private static final String BAD_SOCKET_PATTERN_NO_PORT = 
    "socket://128.195.131.10";
  private static final String BAD_SOCKET_PATTERN_FORMAT = 
    "socket://128.195.131.10/asdfasfasd";
  private static final String BAD_SOCKET_PATTERN_PORT_FORMAT = 
    "socket://128.195.131.10:asdfasfasd";
  
  public void testSuccessfulPattern() throws ResourceException {
    SocketInfo socketInfo = new SocketInfo(SOCKET_PATTERN);
    assertEquals(SOCKET_PATTERN, socketInfo.toString()); 
  }
  
  public void testBadUrlidPattern() {
    try {
      SocketInfo socketInfo = new SocketInfo(BAD_SOCKET_PATTERN_URLID);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Not a socket pattern"));
      return;
    }
    fail("did not recieve ResourceException");
  }
  
  public void testNoPortPattern() {
    try {
      SocketInfo socketInfo = new SocketInfo(BAD_SOCKET_PATTERN_NO_PORT);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Invalid socket pattern"));
      return;
    }
    fail("did not recieve ResourceException");
  }
  
  public void testBadPatternFormat() {
    try {
      SocketInfo socketInfo = new SocketInfo(BAD_SOCKET_PATTERN_FORMAT);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Invalid socket pattern"));
      return;
    }
    fail("did not recieve ResourceException");
  }
  
  public void testBadPatternPortFormat() {
    try {
      SocketInfo socketInfo = new SocketInfo(BAD_SOCKET_PATTERN_PORT_FORMAT);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Invalid port in socket"));
      return;
    }
    fail("did not recieve ResourceException");
  }
  
  public void testEquals() throws ResourceException {
    SocketInfo socketInfo = new SocketInfo(SOCKET_PATTERN);
    SocketInfo socketInfo2 = new SocketInfo(SOCKET_PATTERN);
    assertEquals(socketInfo, socketInfo2);
    
    socketInfo2 = new SocketInfo("socket://0.0.0.0:23");
    assertFalse(socketInfo.equals(socketInfo2));
  }
  
  
}
