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
