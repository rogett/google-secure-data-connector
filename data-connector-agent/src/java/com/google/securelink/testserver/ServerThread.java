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

import com.jcraft.jsch.JSchException;

import java.net.Socket;
import java.io.IOException;

/**
 * Represents a single connection thread to the tunnel-server.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class ServerThread extends Thread {

  // The established socket.
  private Socket socket;
  
  /**
   * Sets up the handler thread.
   * 
   * @param socket an already established connection.
   */
  public ServerThread(Socket socket) {
    super("ServerThread");
    this.socket = socket;
    }

  /**
   * Starts an ssh-client over the socket in a separate thread.
   */
  @Override
  public void run() {

    try {
        SshClient sshClient = new SshClient(socket);
        socket.close();
    } catch (IOException e) {
        ServerConf.log.error(e.getStackTrace());
    } catch (JSchException e) {
        ServerConf.log.error(e.getStackTrace());
    }
  }
} 
