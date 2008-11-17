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
package com.google.dataconnector.registration.v1;

import com.google.dataconnector.registration.v1.ResourceConfigEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Represents a socket resource entry shared by the Secure Link client.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SocketResourceConfigEntry extends ResourceConfigEntry {

  // JSON object keys
  static final String JSON_SECURITY_KEY = "securityKey";
  static final String JSON_PATTERN_KEY = "pattern";

  //public so others can discover what kind of JSON packet this is.
  public static final String JSON_TYPE_KEY = "key";
  public static final String URLID = "socket://";

  private SocketInfo socketInfo;

  /**
   * Creates a socket resource entry from the provided key and pattern.
   * 
   * @param key The unique key both securing and identifying this resource
   * @param pattern The socket pattern in the form of "hostname:port"
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param seqNum the resource seq num
   * @throws ResourceException
   */
  public SocketResourceConfigEntry(final Long key, final String pattern,
      String allowedEntities, final int seqNum) throws ResourceException {
    super(key, seqNum);
    setPattern(pattern);
    setAllowedEntities(allowedEntities);
  }
  
  /**
   * Creates a socket resource entry with a new random key and the provided pattern.
   * 
   * @param pattern The socket pattern in the form of "hostname:port"
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param seqNum the resource seq num
   * @throws ResourceException if pattern is invalid.
   */
  public SocketResourceConfigEntry(final String pattern, String allowedEntities, final int seqNum) 
      throws ResourceException {
    super(new Random().nextLong(), seqNum);
    setPattern(pattern);
    setAllowedEntities(allowedEntities);
  }
  
  /**
   * Creates a socket resource from a JSONObject
   *  
   * @param socketEntryJson A json entry representing a socket resource.
   * @throws ResourceException if pattern is invalid.
   * @throws JSONException if the JSON does not contain the proper keys.
   */
  public SocketResourceConfigEntry(final JSONObject socketEntryJson) throws JSONException, 
      ResourceException {
    super(socketEntryJson.getLong(JSON_SECURITY_KEY), socketEntryJson);
    setPattern(socketEntryJson.getString(JSON_PATTERN_KEY));
    setAllowedEntities(socketEntryJson.getString(JSON_ALLOWEDENTITIES_KEY));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPattern() {
    return socketInfo.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAuthorized(final Long key, final String connectionInfo) {
    try {
      return ((securityKey == key) && (socketInfo.equals(new SocketInfo(connectionInfo))));
    } catch (ResourceException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPattern(final String pattern) throws ResourceException {
    // possible that the pattern has allowedEntities string. string it out
    String[] patternStr = pattern.split(" ");
    socketInfo = new SocketInfo(patternStr[0].trim());
  }
  
  /**
   * {@inheritDoc}
   */
  // TODO(rayc) remove when we have Eugene's HTTP proxy auth change.
  @Override
  public Integer getPort() {
    return socketInfo.getPort();
  }

  /**
   * Returns a JSON Object representing the resource configuration entry. 
   * 
   * @returns a JSON object representing the resource configuration entry.
   */
  @Override
  public JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put(JSON_SEQNUM, seqNum);
      jsonObject.put(JSON_TYPE_KEY, SocketResourceConfigEntry.class.getName());
      jsonObject.put(JSON_SECURITY_KEY, securityKey);
      jsonObject.put(JSON_PATTERN_KEY, getPattern());
      jsonObject.put(JSON_ALLOWEDENTITIES_KEY, getAllowedEntitiesAsString());
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return jsonObject;
  }
  
  /**
   * Convenience class that represents an IP:Port pair that parses a pattern and verifies its 
   * correctness.
   * 
   * @author rayc@google.com (Ray Colline)
   *
   */
  public static class SocketInfo {

    private int port;
    private String hostAddress;

    /**
     * Creates the socket info from from the given pattern.  The pattern must conform to 
     * "hostname:port" convention.
     * 
     * @param pattern A string representing a hostname and port joined by a ":"
     * @throws ResourceException If the supplied pattern is not valid.
     */
    public SocketInfo(String pattern) throws ResourceException {
      
      // Remove socket URL identifier.
      if (pattern.startsWith(URLID)) {
        pattern = pattern.substring(9);
      } else {
        throw new ResourceException("Not a socket pattern: " + pattern);
      }
      
      String[] patternParts = pattern.split(":", 2);
      // We should only have one colon.
      if (patternParts.length != 2) {
        throw new ResourceException("Invalid socket pattern: " + pattern);
      }
      // It should have some length of hostname.
      if (patternParts[0].length() < 1) {
        throw new ResourceException("Invalid socket pattern: " + pattern);
        
      }
      hostAddress = patternParts[0];
      if (!hostAddress.matches("[[\\w\\-]+\\.]+[\\w\\-]")) {
        throw new ResourceException("Invalid host given: " + hostAddress);
      }

      // Parse the string representing the port number.
      try {
        port = new Integer(patternParts[1]);
      } catch (NumberFormatException e) {
        throw new ResourceException("Invalid port in socket pattern: " + pattern);
      }
      // Check port range.
      if ((port < 1) || (port > 65535)) {
        throw new ResourceException("Port out of range in socket pattern: " + pattern);
      }
    }
    
    /**
     * Compares another SocketInfo socket with this one.
     * 
     * @param socketInfo the SocketInfo to compare to.
     * @returns True if they are identical or false if they are not.
     */
    public boolean equals(SocketInfo socketInfo) {
      return ((socketInfo.port == port) && (socketInfo.hostAddress.equals(hostAddress)));
    }

    public int getPort() {
      return port;
    }

    public String getHostAddress() {
      return hostAddress;
    }

    /**
     * @returns a string in the form of "hostname:port"
     */
    @Override
    public String toString() {
      return URLID + hostAddress + ":" + port;
    }
  }
  
  /**
   * Creates a {@link SocketResourceConfigEntry} from a colon separated string entry with the 
   * format, "hostname:ip".  The key is randomly generated. 
   * 
   * @param resourceString String formatted "hostname:ip" representing the entry.
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param seqNum the resource seq num
   * @returns a created object.
   * @throws ResourceException if the format or contents of the string is invalid in anyway.
   */
  public static SocketResourceConfigEntry createEntryFromString(final String resourceString,
      String allowedEntities, int seqNum) 
      throws ResourceException {
    return new SocketResourceConfigEntry(resourceString, allowedEntities, seqNum);
  }
}
