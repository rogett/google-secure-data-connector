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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;

/**
 * Abstract class representing a generic resource entry.  It is expected that any new resources
 * subclass this.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public abstract class ResourceConfigEntry {

  protected static final String JSON_ALLOWEDENTITIES_KEY = "allowedEntities";
  protected static final String JSON_SEQNUM = "seqNum";

  /** Security Key for this resource */
  Long securityKey;
  
  /** seqnum for this resource */
  int seqNum;
  
  /** entities allowed to access this resource - sorted list of strings */
  List<String> allowedEntities;

  /**
   * Sets the random security key & seqNum for this resource.
   * @param key
   */
  public ResourceConfigEntry(final Long key, final JSONObject resourceEntryJson) {
    this.securityKey = key;
    this.seqNum = getSeqNumFromJsonString(resourceEntryJson);
  }
  
  /**
   * Sets the random security key & seqNum for this resource.
   * @param key
   */
  public ResourceConfigEntry(final Long key, final int seqNum) {
    this.securityKey = key;
    this.seqNum = seqNum;
  }
  
  /**
   * Compares pattern and security key of another {@link UriResourceConfigEntry} object
   * 
   * @param entry another instance.
   * @returns true if they have the same configuration.
   */
  public boolean equals(final ResourceConfigEntry entry) {
    return (entry.getSecurityKey().equals(getSecurityKey()) && 
        (entry.getSeqNum() == getSeqNum()) &&
        entry.getPort().equals(getPort()) &&
        ((entry.getAllowedEntities() != null && 
          getAllowedEntities() != null &&
          entry.getAllowedEntities().containsAll(getAllowedEntities())) ||
         (entry.getAllowedEntities() == null && getAllowedEntities() == null)) &&
        entry.getPattern().equals(getPattern()));
  }
  
  /**
   * Checks given connection info and key against entry to see if it is allowed.
   * 
   * @param key The unique key associated with this entry.
   * @param attemptedConnection The string identifier that is being 
   *            connected to.
   * @returns True if supplied key and pattern match and false if it does not.
   */
  public abstract boolean isAuthorized(final Long key, final String attemptedConnection);
  
  /**
   * Sets the pattern of the entry.
   * 
   * @param pattern The pattern to set.
   * @throws ResourceConfigException if pattern is invalid.
   */
  public abstract void setPattern(final String pattern) 
      throws ResourceConfigException;
  
  /**
   * Retrieves the pattern for the entry.
   * 
   * @returns the pattern.
   */
  public abstract String getPattern();
  
  /**
   * Retrieves the port for this resource.
   * 
   * @return the port.
   */
  public abstract Integer getPort();
  
  /**
   * getter for seqNum.
   */
  public Integer getSeqNum() {
    return seqNum;
  }
  
  /**
   * setter for seqNum
   */
  public void setSeqNum(int seqNum) {
    this.seqNum = seqNum;
  }
  
  /** 
   * Retrieves the key
   */
  public Long getSecurityKey() {
    return securityKey;
  }
  
  /**
   * Retrieves the allowedEntities
   */
  public List<String> getAllowedEntities() {
    return allowedEntities;
  }
  
  /**
   * Retrieves the allowedEntities as as a comma separated string
   */
  public String getAllowedEntitiesAsString() {
    StringBuilder sbuf = new StringBuilder();
    boolean firstOne = true;
    for (String s : this.allowedEntities) {
      sbuf.append((!firstOne) ? "," + s : s);
      firstOne = false;
    }
    return sbuf.toString();
  }
  
  /**
   * Sets the AllowedEntities - input is a comma separated set of strings
   */
  public void setAllowedEntities(String allowedEntities) 
      throws ResourceConfigException {
    if (allowedEntities.trim().length() == 0) {
      throw new ResourceConfigException("Invalid value for AllowedEntities");
    }
    this.allowedEntities = Arrays.asList(allowedEntities.split("\\s*,\\s*"));
    Collections.sort(this.allowedEntities);
  }
  
  /**
   * Converts entry into a JSON Object.
   * 
   * @returns a JSON Object representing the resource entry.
   */
  public abstract JSONObject toJSON();
  
  /**
   * returns the seqNum field value from the input Json String - sent by the client
   * 
   * @param jsonString part of the registration request string sent by the client
   * @return the seqNum field value, if it exists. otherwise, 0
   */
  private int getSeqNumFromJsonString(JSONObject jsonString) {
    try {
      return jsonString.getInt(JSON_SEQNUM);
    } catch (JSONException e) {
      // didn't find it. that means it is old client (which doesn't send seqNum)
      return 0;
    }
    
  }
}

