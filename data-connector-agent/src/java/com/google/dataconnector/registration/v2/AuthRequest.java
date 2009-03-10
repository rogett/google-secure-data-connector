/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  static final String USER_KEY = "user";
  static final String DOMAIN_KEY = "domain";
  static final String PASSWORD_KEY = "password";

  private String email;
  private String domain;
  private String user;
  private String password;
  
  // what type of authn mechanism can be used - enumerate
  public static enum AuthType {
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
    json.put(USER_KEY, user);
    json.put(PASSWORD_KEY, password);
    json.put(DOMAIN_KEY, domain);
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public AuthRequest(final JSONObject json) throws JSONException {
    // look for password key. if not found, then let the exception be thrown
    setUser(json.getString(USER_KEY));
    setDomain(json.getString(DOMAIN_KEY));
    setPassword(json.getString(PASSWORD_KEY));
    setAuthType(AuthType.PASSWORD);
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   * 
   * 
   */
  public AuthRequest() {}
  
  public static String getPASSWORD_KEY() {
    return PASSWORD_KEY;
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
