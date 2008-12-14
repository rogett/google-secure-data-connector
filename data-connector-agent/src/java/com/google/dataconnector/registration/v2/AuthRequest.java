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
  static final String USER_KEY = "user";
  static final String DOMAIN_KEY = "domain";
  static final String PASSWORD_KEY = "password";

  public static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
  public static final String URL_FOR_OAUTH = "http://www.google.com/woodstockAgent";
  public static final String OAUTH_REQUESTOR_ID_KEY = "xoauth_requestor_id";
  
  private String oauthString;
  private String email;
  private String domain;
  private String user;
  private String password;
  
  // what type of authn mechanism can be used - enumerate
  public static enum AuthType {
    OAUTH,
    PASSWORD,
    NONE
  }
  
  private AuthType authType; // filled in when JsonString is parsed.

  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    if (oauthString != null) {
      json.put(OAUTH_KEY, oauthString);
    } else {
      json.put(USER_KEY, user);
      json.put(PASSWORD_KEY, password);
      json.put(DOMAIN_KEY, domain);
    }
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public AuthRequest(final JSONObject json) throws JSONException {
    // look for oauth keys
    if (json.has(OAUTH_KEY)) {
      setOauthString(json.getString(OAUTH_KEY));
      setAuthType(AuthType.OAUTH);
    } else {
      // look for password key. if not found, then let the exception be thrown
      setUser(json.getString(USER_KEY));
      setDomain(json.getString(DOMAIN_KEY));
      setPassword(json.getString(PASSWORD_KEY));
      setAuthType(AuthType.PASSWORD);
    }
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
  
  public static String getPASSWORD_KEY() {
    return PASSWORD_KEY;
  }

  public String getOauthString() {
    return oauthString;
  }

  public void setOauthString(String oauthString) {
    this.oauthString = oauthString;
  }
  
  // Readonly.
  public String getEmail() {
    setEmail();
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
  
  /** 
   * Sets the email if both user and domain are available.
   */
  private void setEmail() {
    // These may not be set yet when called.
    if (user == null || domain == null) {
      return;
    }
    email = user + "@" + domain;
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
  
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  public AuthType getAuthType() {
    return authType;
  }

  public void setAuthType(AuthType authType) {
    this.authType = authType;
  }
}
