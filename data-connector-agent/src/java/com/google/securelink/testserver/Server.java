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

package com.google.securelink.testserver;

import java.net.ServerSocket;
import java.io.IOException;

/**
 * Reference implementation of the tunnel-server
 * 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class Server {
  
  // Default port
  public final static String DEFAULT_LISTEN_PORT = "4444";

  /**
   * Creates a TCP server and starts an {@link SshClient} to communicate
   * with the tunnel-service client's sshd.
   * 
   * @param port to listen on
   */
  public Server(int port) {

    ServerSocket serverSocket = null;
    boolean listening = true;
    
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      System.err.println("Could not listen on port:" + port);
      System.exit(-1);
    }

    while (listening)
      try {
        new ServerThread(serverSocket.accept()).start();
      } catch (IOException e) {
        // pass       
      }
  }
  
  /**
   * Entry point for server.
   * 
   * @param args
   */
  public static void main(String[] args) {
    
    try {
      ServerConf.parseFlags(args);
    } catch (ServerException e) {
      ServerConf.log.fatal(e.getMessage());
      System.exit(1);
    }
    
    // Start server
    Server server = new Server(new Integer(
        ServerConf.flags.getOptionValue("p", DEFAULT_LISTEN_PORT)));
  }
}
