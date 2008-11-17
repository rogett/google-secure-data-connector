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
package com.google.dataconnector.registration.v2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * First class object representing AuthRequest for Secure Link protocol.  This provides a 
 * toJson() utility to serialize information for over-the-wire communication and provides
 * a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AuthRequest {
  
  /** Keys used for the Auth Request JSON representation */ 
  static final String OAUTH_KEY = "oauthString";

  public static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
  public static final String URL_FOR_OAUTH = "http://www.google.com/woodstockAgent";
  public static final String OAUTH_REQUESTOR_ID_KEY = "xoauth_requestor_id";
  
  private String oauthString;
  
  // the following are used on the server side
  private String email;
  private String domain;
  private String user;
  
  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(OAUTH_KEY, oauthString);
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public AuthRequest(final JSONObject json) throws JSONException {
    setOauthString(json.getString(OAUTH_KEY));
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   * 
   * 
   */
  public AuthRequest() {}
  
  public static String getOAUTH_KEY() {
    return OAUTH_KEY;
  }

  public String getOauthString() {
    return oauthString;
  }

  public void setOauthString(String oauthString) {
    this.oauthString = oauthString;
  }
  
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}