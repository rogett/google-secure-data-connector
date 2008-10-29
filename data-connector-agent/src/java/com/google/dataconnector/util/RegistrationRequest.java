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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * First class object representing the Registration request for the Secure Link protocol.  This 
 * provides a toJson() utility to serialize information for over-the-wire communication and 
 * provides a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationRequest {

  private static final Logger log = Logger.getLogger(RegistrationRequest.class);

  // Support old client names. for "securelink" branded code.
  private static final String OLD_V1_SOCKET_ENTRY_NAME = 
      "com.google.securelink.util.SocketResourceConfigEntry";
  private static final String OLD_V1_URL_ENTRY_NAME = 
      "com.google.securelink.util.UriResourceConfigEntry";

  static final String RESOURCES_KEY = "resources";
  static final String CLIENT_ID_KEY = "clientId";
  static final String SOCKS_PORT = "socksPort";
  
  private JSONArray resourcesJsonArray;
  private String clientId;
  private Integer socksPort;
  
  /**
   * Creates the request from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public RegistrationRequest(final JSONObject json) throws JSONException {
    this.resourcesJsonArray = json.getJSONArray(RESOURCES_KEY);
    this.clientId = json.getString(CLIENT_ID_KEY);
    this.socksPort = json.getInt(SOCKS_PORT);
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   */
  public RegistrationRequest(final ClientConf clientConf) {
    resourcesJsonArray = toResourceJsonArray(clientConf.getRules());
    clientId = clientConf.getClient_id();
    socksPort = clientConf.getSocksServerPort();
  }

  /**
   * Returns all socket resources contained in the registration request.
   * 
   * @return a collection of socket config entries.
   * @throws JSONException if any invalid or incorrectly formulated JSON is encountered
   * @throws ResourceConfigException if the data within the socket resources is invalid.
   */
  public Collection<SocketResourceConfigEntry> getAllSocketResources() throws JSONException, 
      ResourceConfigException {
    Collection<SocketResourceConfigEntry> socketResources = 
        new ArrayList<SocketResourceConfigEntry>();
    for(int index=0; index < resourcesJsonArray.length(); index++) {
      JSONObject resourceInfoJson = resourcesJsonArray.getJSONObject(index);
      if (resourceInfoJson.has(SocketResourceConfigEntry.JSON_TYPE_KEY) && 
              (resourceInfoJson.get(SocketResourceConfigEntry.JSON_TYPE_KEY).equals(
                  SocketResourceConfigEntry.class.getName()) ||
              resourceInfoJson.get(SocketResourceConfigEntry.JSON_TYPE_KEY).equals(
                  OLD_V1_SOCKET_ENTRY_NAME))) {
        socketResources.add(new SocketResourceConfigEntry(resourceInfoJson));
      }
    }
    return socketResources;
  }

  /**
   * Returns all URI resources contained in the registration request.
   * 
   * @return a collection of URI config entries.
   * @throws JSONException if any invalid or incorrectly formulated JSON is encountered
   * @throws ResourceConfigException if the data within the uri resources is invalid.
   */
  public Collection<UriResourceConfigEntry> getAllUriResources() throws JSONException, 
      ResourceConfigException {
    Collection<UriResourceConfigEntry> uriResources = new ArrayList<UriResourceConfigEntry>();
    for(int index=0; index < resourcesJsonArray.length(); index++) {
      JSONObject resourceInfoJson = resourcesJsonArray.getJSONObject(index);
      if (resourceInfoJson.has(UriResourceConfigEntry.JSON_TYPE_KEY) && 
              (resourceInfoJson.get(UriResourceConfigEntry.JSON_TYPE_KEY).equals(
                  UriResourceConfigEntry.class.getName()) ||
              resourceInfoJson.get(UriResourceConfigEntry.JSON_TYPE_KEY).equals(
                  OLD_V1_URL_ENTRY_NAME))) {
        uriResources.add(new UriResourceConfigEntry(resourceInfoJson));
      }
    }
    return uriResources;
  }

  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(RESOURCES_KEY, resourcesJsonArray);
    json.put(CLIENT_ID_KEY, clientId);
    json.put(SOCKS_PORT, socksPort);
    return json;
  }
  
  /**
   * Generate {@link JSONArray} containing the JSON representation of each 
   * {@link ResourceConfigEntry} in the provided list.
   * 
   * @param resourceEntryList list of resources.
   * @return JSONArray of JSON entries representing resources.
   */
  static JSONArray toResourceJsonArray(List<ResourceConfigEntry> resourceEntryList) {
    JSONArray configJsonArray = new JSONArray();
    for (ResourceConfigEntry entry : resourceEntryList) {
      configJsonArray.put(entry.toJSON());
    }
    return configJsonArray;
  }
  
  /**
   * Compares this instance against supplied instance, by looking at the internal
   * relevant fields and the Resources JSON array (as a String) to see if they are equal.
   * 
   * @returns boolean indicating equivalence or not.
   */
  @Override
  public boolean equals(Object in) {
    
    try {
      RegistrationRequest inRegRequest = (RegistrationRequest) in;
      
      try {
        // compare socket resources
        // we know the following returns ArrayList - so cast it. 
        ArrayList<SocketResourceConfigEntry> socketResources1 = 
            (ArrayList<SocketResourceConfigEntry>)inRegRequest.getAllSocketResources();
        ArrayList<SocketResourceConfigEntry> socketResources2 = 
            (ArrayList<SocketResourceConfigEntry>)getAllSocketResources();
        // look for one-to-one equality between the above two ArrayLists
        // this is very strict equality checking, eh.
        if (socketResources1.size() != socketResources2.size()) {
          return false;
        }
        if (socketResources1.size() > 0) {
          for (int i = 0; i < socketResources1.size(); i++) {
            if (!socketResources1.get(i).equals(socketResources2.get(i))) {
              return false;
            }
          }
        }
        
        // compare http resources
        ArrayList<UriResourceConfigEntry> uriResources1 = 
            (ArrayList<UriResourceConfigEntry>)inRegRequest.getAllUriResources();
        ArrayList<UriResourceConfigEntry> uriResources2 = 
            (ArrayList<UriResourceConfigEntry>)getAllUriResources();
        // look for one-to-one equality between the above two ArrayLists
        // this is very strict equality checking, eh.
        if (uriResources1.size() != uriResources2.size()) {
          return false;
        }
        if (uriResources1.size() > 0) {
          for (int i = 0; i < uriResources1.size(); i++) {
            if (!uriResources1.get(i).equals(uriResources2.get(i))) {
              return false;
            }
          }
        }
      } catch (ResourceConfigException e) {
        // this shouldn't really happen
        log.error(e.getMessage());
        return false;
      } catch (JSONException e) {
        // this shouldn't really happen
        log.error(e.getMessage());
        return false;
      }
      return (inRegRequest.getClientId().equals(getClientId()) &&
          inRegRequest.getSocksPort().equals(getSocksPort()));
    } catch (ClassCastException e) {
      return false;
    }
  }
  
  // Getters and setters
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public JSONArray getResources() {
    return resourcesJsonArray;
  }

  public void setResources(JSONArray resources) {
    this.resourcesJsonArray = resources;
  }

  public Integer getSocksPort() {
    return socksPort;
  }

  public void setSocksPort(Integer socksPort) {
    this.socksPort = socksPort;
  }
  // End Getters and setters
}
