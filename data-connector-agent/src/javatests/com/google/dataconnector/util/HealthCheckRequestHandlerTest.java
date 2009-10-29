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
 * Tests for the {@link HealthCheckRequestHandler} class.
 *
 * @author vnori@google.com (Vasu Nori)
 *
 */
public class HealthCheckRequestHandlerTest extends TestCase {

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

  public void testHealthCheckRequestHandlerProcessingRequest() throws IOException {

    ShutdownManager shutdownManager = EasyMock.createMock(ShutdownManager.class);
    shutdownManager.addStoppable(EasyMock.isA(Stoppable.class));
    EasyMock.expectLastCall();
    EasyMock.replay(shutdownManager);
    
    // Successful case.
    InputStream is = new ByteArrayInputStream(("GET /healthcheck\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    ServerSocket mockServerSocket = EasyMock.createMock(ServerSocket.class);
    EasyMock.expect(mockServerSocket.accept()).andReturn(getFakeSocket(is, os));
    EasyMock.expect(mockServerSocket.accept()).andThrow(new IOException("test done"));
    EasyMock.replay(mockServerSocket);
    HealthCheckRequestHandler testHealthCheckRequestHandler =
        new HealthCheckRequestHandler(mockServerSocket, shutdownManager);
    try {
      testHealthCheckRequestHandler.run();
      String strOut = ((ByteArrayOutputStream) os).toString("utf8");
      assertTrue(strOut.contains("ok"));
      assertTrue(strOut.contains("HTTP/1.1 200 OK"));
      assertTrue(strOut.contains("Server: SDC_agent"));
      assertTrue(strOut.contains("Cache-Control: no-cache"));
      assertTrue(strOut.contains("Pragma: no-cache"));
      assertTrue(strOut.contains("Content-Length: 2"));
    } catch (IOException e) {
      if (!e.getMessage().contains("test done")) {
        fail(e.getMessage());
      }
    }
    EasyMock.verify(mockServerSocket, shutdownManager);
  }
}
