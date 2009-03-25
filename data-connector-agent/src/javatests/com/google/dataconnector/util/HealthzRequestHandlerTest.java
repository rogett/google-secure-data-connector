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
package com.google.dataconnector.util;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Tests for the {@link HealthzRequestHandler} class.
 * 
 * @author vnori@google.com (Vasu Nori)
 *
 */
public class HealthzRequestHandlerTest extends TestCase {

  /**
   * Creates mock socket from the input streams provided for use with the registration util.
   * 
   * @param is pre populated input stream such as a {@link ByteArrayInputStream}
   * @param os pre populated output stram such as a {@link ByteArrayOutputStream}
   * @return configured mock socket.
   * @throws IOException to satisfy Socket class.
   */
  private Socket getFakeSocket(InputStream is, OutputStream os) throws IOException {
    Socket fakeSocket = EasyMock.createMock(Socket.class);
    EasyMock.expect(fakeSocket.getInputStream()).andReturn(is);
    EasyMock.expect(fakeSocket.getOutputStream()).andReturn(os);
    EasyMock.replay(fakeSocket);
    return fakeSocket;
  }
  
  public void testHealthzRequestHandlerProcessingRequest() throws IOException {
    
    // Successful case.
    InputStream is = new ByteArrayInputStream(("GET /healthz\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    ServerSocket mockServerSocket = EasyMock.createMock(ServerSocket.class);
    EasyMock.expect(mockServerSocket.accept()).andReturn(getFakeSocket(is, os));
    EasyMock.expect(mockServerSocket.accept()).andThrow(new IOException("test done"));
    EasyMock.replay(mockServerSocket);
    HealthzRequestHandler testHealthzRequestHandler = new HealthzRequestHandler(mockServerSocket);
    try {
      testHealthzRequestHandler.run();
      String strOut = ((ByteArrayOutputStream) os).toString("utf8");
      assertTrue(strOut.startsWith("ok"));
    } catch (IOException e) {
      if (!e.getMessage().contains("test done")) {
        fail(e.getMessage());
      }
    }
    EasyMock.verify(mockServerSocket);
  }
}
