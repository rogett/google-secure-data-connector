package com.google.dataconnector.registration.v2;

import com.google.dataconnector.registration.v2.ResourceException;

/**
 * Convenience class that represents an IP:Port pair that parses a pattern and verifies its 
 * correctness.
 * 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class SocketInfo {

  private int port;
  private String hostAddress;
  
  private static final String URLID = "socket://";

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
   * @param s2 the SocketInfo to compare to.
   * @returns True if they are identical or false if they are not.
   */
  @Override
  public boolean equals(Object s2) {
    if (s2 instanceof SocketInfo) {
      return ((((SocketInfo) s2).getPort() == port) && 
          (((SocketInfo) s2).hostAddress.equals(hostAddress)));
    } else {
      return false;
    }
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