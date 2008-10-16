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

package net.sourceforge.jsocks.socks.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import net.sourceforge.jsocks.SOCKS;
import net.sourceforge.jsocks.socks.AuthenticationException;
import net.sourceforge.jsocks.socks.AuthenticationException.AuthErrorType;
import net.sourceforge.jsocks.socks.ProxyMessage;

/**
 * An implementation of {@link ServerAuthenticator} that allows a key/password
 * to be associated with a given IP,port combination.  It will only allow
 * that given password to proxy to its associated IP,port combination.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class PasswordPerIpAuthenticator extends ServerAuthenticatorNone {

  // METHOD ID
  private final static int METHOD_ID = 0x79;
   
  // HashMap that stores all the rules for this SOCKS fire-wall.
  private HashMap<String,List<IpPortPair>> rulesMap;
   
  // Key used to authenticate this request
  private String passKey;
   
  /**
   * Constructs empty PasswordPerIpAuthenticator.
   */
  public PasswordPerIpAuthenticator() {
    rulesMap = new HashMap<String,List<IpPortPair>>();
  }
   
  /**
   * Used to create instances returned from startSession.
   * 
   * @param in Input stream.
   * @param out OutputStream.
   * @param passKey the password provided by the client for this connection
   * @param rulesMap the list of rules that are allowed for this server config.
   */
  PasswordPerIpAuthenticator(InputStream in,OutputStream out, String passKey,
      HashMap<String,List<IpPortPair>> rulesMap) {
    super(in,out);
    this.passKey = passKey;
    this.rulesMap = rulesMap;   
  }

  /**
   * Adds a rule to the authenticator associating a password with an IP port
   * pair
   * 
   * @param key the key/password to associate with this rule
   * @param ip a dotted-quad IP address.
   * @param port an integer representing the allowed port.
   * 
   */
  public synchronized void add(String key, String ip, Integer port) {
      IpPortPair ipPortPair = new IpPortPair(ip, port);
      if (rulesMap.containsKey(key)) {
        rulesMap.get(key).add(ipPortPair);
      } else {
        List<IpPortPair> ipPortPairList = new ArrayList<IpPortPair>();
        ipPortPairList.add(ipPortPair);
        rulesMap.put(key, ipPortPairList);
      }
  }

  /**
   * Gathers the passKey from the client request and returns the 
   * ServerAuthenticator.  The real check is done in the "checkRequest"
   * step.
   */
  @Override
  public ServerAuthenticator startSession(Socket s)throws IOException {

     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream();
     
     // Reject non SOCKS5 requests.
     if (in.read() != 5) {
       return null; 
     }

     // We only support our custom auth method.  Reject others.
     if (!selectSocks5Authentication(in,out,METHOD_ID)) {
       return null;
     }
     
     // Read the key from the request  
     try {
       passKey = new String(getKeyFromRequest(s, in, out));
     } catch (AuthenticationException e) {
       SOCKS.inform("Invalid Authentication: " + e.getMessage());
       return null; 
     }
     // Verify passKey
     if (rulesMap.containsKey(passKey)) {
       // we have a password that matches, we will check the dest later
       out.write(new byte[]{1,0});
     } else {
       // failed auth, we have no passwords that match
       out.write(new byte[]{1,1});
     }
     
     return new PasswordPerIpAuthenticator(in,out, passKey, rulesMap);
   }
  
   /**
    * Checks the destination IP and port specified in the {@link ProxyMessage}
    * against the allowed IP:Port pair for this connection.  If its valid allow
    * the connection to continue.  Reject all non SOCKS 5 requests.
    * 
    * @param msg
    * @param s
    * @return
    */
   @Override
   public boolean checkRequest(ProxyMessage msg) {

     // This shouldn't happen but we should check anyways.
     if (msg.version != 5) {
       return false;
     }

     List<IpPortPair> ipPortPairList = rulesMap.get(passKey);

     if (ipPortPairList == null) {
       return false;
     }
     
     for (IpPortPair ipPortPair: ipPortPairList) {
       if ((msg.host.equals(ipPortPair.getIp()) && 
           (msg.port == ipPortPair.getPort()))) {
         return true;
       } 
     }
     return false;
   }

  /** Get String representation of the PerPasswordIpAuthenticator including
   * current key and rulesMap.
   */
  @Override
  public String toString() {
     String s = "Given Passkey: " + passKey + "\n";
     
     for (String key : rulesMap.keySet()) {
       for (IpPortPair ipPortPair : rulesMap.get(key)) {
         s += "Key: " + key + " Dest: " + ipPortPair.getIp() + ":" +
             ipPortPair.getPort() + "\n";
       }
     }
     return s;
  }

  /**
   * Retrieves the key from the client request.
   * 
   * @param s socket connected to the client
   * @param in input from client
   * @param out output to client
   * @return the key.
   * @throws AuthenticationException if version is not 1 or if the key length
   *             is less than 0.q
   * @throws IOException on any read/write socket errors.
   */
  private byte[] getKeyFromRequest(Socket s, InputStream in,
      OutputStream out) throws AuthenticationException, IOException {
    
    int version = in.read();
    if(version != 1) {
      throw new AuthenticationException(AuthErrorType.MALFORMED_REQUEST);
    }
    int klen = in.read();
    if(klen < 0) {
      throw new AuthenticationException(AuthErrorType.MALFORMED_REQUEST);
    }
    byte[] key = new byte[klen];
    in.read(key);
    return key;
  }
  
  /**
   * Inner class to represent a the ip port pair.
   * 
   * @author rayc@google.com (Ray Colline)
   *
   */
  public class IpPortPair {
    
    // IP address in dot form most likely
    private String ip;
    
    // Port
    private int port;
    
    /**
     * Simple constructor 
     * 
     * @param ip a string representing a DNS name or dotted IP address
     * @param port an int representing a TCP port.
     */
    public IpPortPair(String ip, int port) {
      this.ip = ip;
      this.port = port;
    }
    
    /**
     * Returns the ip associated with this pair.
     */
    public String getIp() {
      return this.ip;
    }

    /**
     * Returns the port associated with this pair.
     */
    public int getPort() {
      return this.port;
    }
  }
    
}
