/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA 
 */

package net.sourceforge.jsocks.socks;

import net.sourceforge.jsocks.socks.AuthenticationException.AuthErrorType;

/**
 * SOCKS5 Password per IP authentication scheme.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class PasswordPerIpAuthentication implements Authentication {

   // SOCKS ID for User/Password authentication method
   public final static byte METHOD_ID = 0x79;

   // Shared secret for this request.
   String password;
   
   // The PasswordPerIp request
   byte[] request;

   /**
    * Create an instance of PasswordPerIpAuthenticator.
    * 
    * @param password Password to send to SOCKS server.
    */
   public PasswordPerIpAuthentication(String password) {
     /* TODO(rayc) Add length check to password to avoid > 255 chars and
      * probably throw an exception
      */
     this.password = password;
     this.request = formRequest();
   }

   /** Gets the password associated with this connection.
    * 
    * @return the password used to authenticate this connection
    */
   public String getPassword() {
     return password;
   }
   
   /**
    * Does Password authentication
    * 
    * @return An array containing in, out streams, or null if authentication
    * fails.
    */
   public Object[] doSocksAuthentication(int methodId,
       java.net.Socket proxySocket) throws java.io.IOException {

      if(methodId != METHOD_ID) return null;

      java.io.InputStream in = proxySocket.getInputStream();
      java.io.OutputStream out = proxySocket.getOutputStream();

      out.write(request);
      int version = in.read();
      if(version < 0) return null; //Server closed connection
      int status = in.read();
      if(status != 0) return null; //Server closed connection, or auth failed.

      return new Object[] {in,out};
   }

   /** 
    * Converts the password in to binary form needed for sending 
    * to the server.
    * 
    * @returns the formed request
    */
   private byte[] formRequest() {
      byte[] password_bytes = password.getBytes();
      byte [] request = new byte[2+password_bytes.length];
      request[0] = (byte) 1; 
      request[1] = (byte) password_bytes.length;
      System.arraycopy(password_bytes,0,request,2,password_bytes.length);
      return request;
   }
}
