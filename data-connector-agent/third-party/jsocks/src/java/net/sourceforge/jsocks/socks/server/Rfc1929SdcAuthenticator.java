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

import net.sourceforge.jsocks.socks.ProxyMessage;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class implements both authentication and Layer 4 IP rule enforcement for incoming SOCKS5 
 * connections.  It uses the RFC1929 specification however the user name is a JSON packet used a 
 * log vehicle, while the password is used a key that only allows access to the specified rule 
 * sets in the configuration.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Rfc1929SdcAuthenticator extends ServerAuthenticatorNone {

   private static final Logger LOG = Logger.getLogger(Rfc1929SdcAuthenticator.class);
   
   // RFC1929 Method ID
   static final int METHOD_ID = 2;

   // HashMap that stores all the layer 4 rules associated with their unique key.
   private HashMap<String,List<IpPortPair>> rulesMap;
   
   private String passKey; // RFC1929 password used to lookup rule set in the map.
   private String serverMetaData; // Raw JSON string from server

   /**
    * Used to create instances returned from startSession in JSOCKS server..
    * 
    * @param in InputStream of socks connection.
    * @param out OutputStream of socks connection.
    * @param passKey the password provided by the client for this connection
    * @param rulesMap the list of rules that are allowed for this server config.
    */
   Rfc1929SdcAuthenticator(InputStream in,OutputStream out, String passKey, 
       HashMap<String,List<IpPortPair>> rulesMap, String userLogLine) {
     super(in,out);
     this.passKey = passKey;
     this.rulesMap = rulesMap;   
     this.serverMetaData = userLogLine;
   }
   
   /**
    * Initialize passkey map.
    */
   public Rfc1929SdcAuthenticator(){
      rulesMap = new HashMap<String,List<IpPortPair>>();
   }
   
  /**
   * Adds a rule to the authenticator associating a password with an IP port pair.
   * 
   * @param key the key/password to associate with this rule
   * @param ip a dotted-quad IP address.
   * @param port an integer representing the allowed port.
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
   * Checks the destination IP and port specified in the {@link ProxyMessage} against the allowed 
   * IP:Port pair for this connection.  If its valid allow the connection to continue.  Reject all
   * non SOCKS 5 requests.  We also log  any cloud provided data if we have any.
   * 
   * @param msg the JSOCKS msg object representing the SOCKS connection parameters.
   * @returns true if request is allowed or false if its prevented by ruleset.
   */
  @Override
  public boolean checkRequest(ProxyMessage msg) {

    // This shouldn't happen but we should check anyways.
    if (msg.version != 5) {
      return false;
    }

    try {
      JSONObject serverMetadataJson = new JSONObject(serverMetaData);
      String name = serverMetadataJson.getString("name");
      String resource = serverMetadataJson.getString("resource");
      String user = serverMetadataJson.getString("user");
      String appId = serverMetadataJson.getString("appId");
      LOG.info(msg.getConnectionId() + " Incoming connection for rule id:" +  name + 
          " for resource:" + resource +  " cloud-user:" + user + " reported-appId:" + appId);
    } catch (JSONException e) {
      LOG.info(msg.getConnectionId() + " Cloud did not report metadata (old cloud clients?)");
    }

    // Is this a valid "secret key"
    List<IpPortPair> ipPortPairList = rulesMap.get(passKey);
    if (ipPortPairList == null) {
      LOG.warn(msg.getConnectionId() + " Auth failed to " + msg.host + ":" + msg.port + 
          " for rule ");
      return false;
    }

    // Check to see if the connection is allowed via IP rule set for this "secret key".
    for (IpPortPair ipPortPair: ipPortPairList) {
      if ((msg.host.equals(ipPortPair.getIp()) && (msg.port == ipPortPair.getPort()))) {
        LOG.info(msg.getConnectionId() + " Authentication successful");
        return true;
      } 
    }
    return false;
  }
  
  /**
   * Reads the authentication data from the session and validates request
   * 
   * @param s connected socket from incoming SOCKS client.
   * @throws IOException if there are any socket communication issues.
   */
  @Override
  public ServerAuthenticator startSession(Socket s) throws IOException {
    InputStream in = s.getInputStream();
    OutputStream out = s.getOutputStream();

    if(in.read() != 5) return null; //Drop non version 5 messages.

    if(!selectSocks5Authentication(in,out,METHOD_ID)) 
      return null;
    if(!doUserPasswordAuthentication(s,in,out))
      return null;

    return new Rfc1929SdcAuthenticator(in, out, passKey, rulesMap, serverMetaData);
  }
  
  /** 
   * Get String representation of the PerPasswordIpAuthenticator including current key and rulesMap.
   */
  @Override
  public String toString() {
     String s = "Given Passkey: " + passKey + "\n";
     
     for (String key : rulesMap.keySet()) {
       for (IpPortPair ipPortPair : rulesMap.get(key)) {
         s += "Key: " + key + " Dest: " + ipPortPair.getIp() + ":" + ipPortPair.getPort() + "\n";
       }
     }
     return s;
  }

  /**
   * Helper function that actually performs the authentication.
   * 
   * @param s connected socket from SOCKS client.
   * @param in input stream of socket.
   * @param out output stream of socket.
   * @returns true if authorized false if not.
   * @throws IOException if there any socket communication issues.
   */
  private boolean doUserPasswordAuthentication(Socket s,
                                               InputStream in,
                                               OutputStream out) 
                                               throws IOException {
    int version = in.read();
    if(version != 1) return false;
    // This byte tells us how many bytes the username will be.  Max is 255.
    int ulen = in.read();
    if(ulen < 0) return false;
    byte[] user = new byte[ulen];
    in.read(user);
    // This byte tells us how many bytes the password will be.  Max is 255.
    int plen = in.read();
    if(plen < 0) return false;
    byte[] password = new byte[plen];
    in.read(password);

    // We use the RFC1929 Username to pass metadata to the client about this connection.
    serverMetaData = new String(user);
    passKey = new String(password);
    
    // Verify passKey exists.
    if (rulesMap.containsKey(passKey)) {
      // we have a passkey that matches, we will check the dest later
      out.write(new byte[]{1,0});
    } else {
      // failed auth, we have no passwords that match
      out.write(new byte[]{1,1});
      return false;
    }
    return true;
  }

  /**
   * Class to represent a the ip port pair.
   * 
   * @author rayc@google.com (Ray Colline)
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
