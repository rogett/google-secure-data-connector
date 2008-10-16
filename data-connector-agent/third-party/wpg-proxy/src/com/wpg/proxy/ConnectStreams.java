/*
 * Java HTTP Proxy Library (wpg-proxy), more info at
 * http://wpg-proxy.sourceforge.net/
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * 
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package com.wpg.proxy;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Class that allocates a thread to move data from an {@link InputStream} to an
 * {@link OutputStream} in 64k blocks.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ConnectStreams extends Thread {
  private static Logger logger = Logger.getLogger(ConnectStreams.class);
  // Input stream to read from
  private InputStream in;

  // Output stream to write to.
  private OutputStream out;

  /**
   * Creates a {@link ConnectStreams} object.
   * 
   * @param in stream to read from
   * @param out stream to write to.
   * @param id thread name for identification.
   */
  public ConnectStreams(InputStream in, OutputStream out, String id) {
    this.in = in;
    this.out = out;
    this.setName(id);
  }

  /**
   * Creates a {@link ConnectStreams} object.
   * 
   * @param inSocket socket to read from
   * @param outSocket socket to write to.
   * @param id thread name for identification.
   */
  public ConnectStreams(Socket inSocket, Socket outSocket, String id) 
      throws IOException {
    this(inSocket.getInputStream(), outSocket.getOutputStream(), id);
  }

  @Override
  /**
   * Reads up to 64k from the InputStream and writes it to the OutputStream. The
   * thread blocks when data is not available.
   */
  public void run() {
    try {
      synchronized (in) {
        synchronized (out) {
          byte[] buffer = new byte[65536];
          while (true) {
            logger.debug(getName() + " About to read");
            int bytesRead = in.read(buffer);
            if (bytesRead >= 0) {
              logger.debug(getName() + "********************* Read " + bytesRead
                  + " bytes. . Value=" + new String(buffer, 0, bytesRead));
            } else {
              logger.debug(getName() + " Conection closed");
              break;

            }
            logger.debug("About to send " + bytesRead + " bytes.");
            out.write(buffer, 0, bytesRead);
            logger.debug(getName() + " Wrote " + bytesRead + " bytes");
            out.flush();
          }
        }
      }
    } catch (IOException e) {
      logger.debug(getName() + " had an error", e);
      // We only print this error when debug is on because this exception will
      // be thrown very often, as connections can be closed by the either
      // end of the conversation. We don't want to contaminate the error log
      // as this exception is expected on a regular basis.
    } finally {
      try {
        in.close();
        out.close();
        logger.debug(getName() + " is finishing execution");
      } catch (IOException e) {
        logger.error(getName() + " Error closing streams ", e);
      }
    }

  }
}
