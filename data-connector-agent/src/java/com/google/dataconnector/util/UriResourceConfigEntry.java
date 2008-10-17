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

import com.google.dataconnector.util.ResourceConfigEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Represents a URI resource entry shared by the Secure Link client.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class UriResourceConfigEntry extends ResourceConfigEntry {

  // JSON object keys
  static final String JSON_SECURITY_KEY = "securityKey";
  static final String JSON_URI_PATTERN_KEY = "uriPattern";
  static final String JSON_PROXY_PORT_KEY = "proxyPort";

  //public so others can discover what kind of JSON packet this is.
  public static final String JSON_TYPE_KEY = "key";
  public static final String HTTPURLID = "http://"; 
  public static final String HTTPSURLID = "https://"; 
  
  private String uriPattern;
  private int proxyPort;

  /**
   * Creates a socket resource entry from the provided key and pattern.
   * TODO This constructor is probably just for testing purposes. 
   * remove all such "for testing only" code into a separate section of the project
   * to reduce the code needed for regular run.
   * 
   * @param key The unique key both securing and identifying this resource
   * @param pattern The URI pattern in the form of "http://foo.*.org/bar"
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param proxyPort The port the Secure Link client http proxy is listening on.
   * @param seqNum the resource seq num
   * @throws ResourceConfigException
   */
  public UriResourceConfigEntry(final Long key, final String pattern, 
      String allowedEntities, final int proxyPort, final int seqNum) 
      throws ResourceConfigException {
    super(key, seqNum);
    setPattern(pattern);
    setAllowedEntities(allowedEntities);
    this.proxyPort = proxyPort;
  }
  
  /**
   * Creates a socket resource entry from the provided pattern but randomly 
   * generates a key.
   * 
   * @param pattern The URI pattern in the form of "http://foo.*.org/bar "
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param proxyPort The port the Secure Link client http proxy is listening on.
   * @param seqNum the resource seq num
   * @throws ResourceConfigException if pattern is invalid.
   */
  public UriResourceConfigEntry(final String pattern, String allowedEntities, final int proxyPort,
      final int seqNum) throws ResourceConfigException {
    super(new Random().nextLong(), seqNum);
    setPattern(pattern);
    setAllowedEntities(allowedEntities);
    this.proxyPort = proxyPort;
  }
  
  /**
   * Creates a socket resource from a JSONObject with the proper format defined
   * by the "JSON_" constants above.
   * 
   * @param uriEntryJson A json entry representing a socket resource.
   * @throws ResourceConfigException if uri pattern is invalid
   * @throws JSONException if the JSON does not contain the proper keys.
   */
  public UriResourceConfigEntry(final JSONObject uriEntryJson) 
      throws JSONException, ResourceConfigException {
    super(uriEntryJson.getLong(JSON_SECURITY_KEY), uriEntryJson);
    setPattern(uriEntryJson.getString(JSON_URI_PATTERN_KEY));
    setAllowedEntities(uriEntryJson.getString(JSON_ALLOWEDENTITIES_KEY));
    proxyPort = uriEntryJson.getInt(JSON_PROXY_PORT_KEY);
  }
  
  @Override
  public String getPattern() {
    return uriPattern.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAuthorized(final Long key, final String connectionInfo) {
    if (securityKey != key) {
      return false;
    }
    if (connectionInfo.matches(uriPattern.toString())) {
      return true; 
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPattern(final String pattern) throws ResourceConfigException {
    if (pattern.startsWith(HTTPURLID) || pattern.startsWith(HTTPSURLID)) {
      uriPattern = pattern;
    } else {
      throw new ResourceConfigException("Not an http pattern.  Must start with http:// or " +
          "https:// Offending pattern: " + pattern);
    }
  }

  /**
   * {@inheritDoc}
   */
  // TODO(rayc) remove when we have Eugene's HTTP proxy auth change.
  @Override
  public Integer getPort() {
    return proxyPort;
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
      jsonObject.put(JSON_TYPE_KEY, UriResourceConfigEntry.class.getName());
      jsonObject.put(JSON_SECURITY_KEY, securityKey);
      jsonObject.put(JSON_URI_PATTERN_KEY, uriPattern.toString());
      jsonObject.put(JSON_PROXY_PORT_KEY, proxyPort);
      jsonObject.put(JSON_ALLOWEDENTITIES_KEY, getAllowedEntitiesAsString());
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return jsonObject;
  }
  
  /**
   * Creates a {@link UriResourceConfigEntry} from a colon separated string entry with the 
   * format, "hostname:ip".  The key is randomly generated. 
   * 
   * @param propertyString String formatted "hostname:ip" representing the entry.
   * @param allowedEntities comma separated list of the allowed entities (user1,group1)
   * @param proxyPort The Secure Link http proxy port associated with this URI.
   * @param seqNum the resource seq num
   * @returns a created object.
   * @throws ResourceConfigException if the format or contents of the string is invalid in anyway.
   */
  public static UriResourceConfigEntry createEntryFromString(final String propertyString, 
      String allowedEntities, final int proxyPort, int seqNum) throws ResourceConfigException {
    return new UriResourceConfigEntry(propertyString, allowedEntities, proxyPort, seqNum);
  }
}
