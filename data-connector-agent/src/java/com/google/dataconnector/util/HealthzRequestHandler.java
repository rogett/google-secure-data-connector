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

import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HealthzRequestHandler extends Thread {
  
  public static Logger LOG = Logger.getLogger(HealthzRequestHandler.class.getName());
  
  private ServerSocket serverSocket;
  private boolean quit = false;

  @Inject
  public HealthzRequestHandler(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }
  
  /**
   * Returns the port this service is listening on. This is used when system rules such as
   * healthz service rule are added to the user-defined list of resource rules.
   *  
   * @return the port the healthz service is listening on.
   */
  public int getPort() {
    if (serverSocket == null) {
      return -1;
    }
    return serverSocket.getLocalPort();
  }
  
  /**
   * Initializes the HealthzRequestHandler in a separate thread after opneing a ServerSocket
   * 
   * @throws IOException propagated from ServerSocket creation, if any exception
   */
  public void init() throws IOException {
    
    // make this a daemon thread, so it quits when non-daemon threads exit.
    // TODO: make all daemon threads non-daemons and make them quit when they are told to,
    // for example by calling setQuitFlag() in this class to make this thread exit.
    setDaemon(true);
    
    // start the service in a separate thread
    start();
    LOG.info("healthz service started on port " + getPort());
  }
  
  /**
   * the run method of the HealthzRequestHandler thread. It waits to receive a socket connect 
   * request from the callers wishing to send "GET /healthz" http request. 
   * upon receiving the /healthz request, it responds with a simple "ok" response.
   */
  @Override
  public void run() {
    setName("HealthzRequestHandler");
    try {
      while (!quit) {
        Socket incomingSocket = serverSocket.accept();

        // read incoming request
        BufferedReader in = new BufferedReader(new InputStreamReader(
            incomingSocket.getInputStream()));
        PrintWriter out = new PrintWriter(incomingSocket.getOutputStream(), true);
        String inputStr;
        while ((inputStr = in.readLine()) != null) {
          if (inputStr.contains("/healthz")) {
            LOG.debug("healthz req found");
            break;
          }
        }
        out.println("ok");
        out.close();
        LOG.debug("processed healthz request");
      }
    } catch (IOException e) {
      LOG.warn("Healthz service IOException", e);
    }
  }

  /**
   * this method is called to let the thread know that it should quit.
   */
  public void setQuitFlag() {
    this.quit = true;
  }
}
