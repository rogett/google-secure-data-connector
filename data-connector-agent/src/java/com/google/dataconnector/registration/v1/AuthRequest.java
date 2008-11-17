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

  private String user;
  private String domain;
  private String password;

  // Read only variable that represents the domain admin email but is not part of the Json request.
  private String email;

  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(USER_KEY, user);
    json.put(DOMAIN_KEY, domain);
    json.put(PASSWORD_KEY, password);
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public AuthRequest(final JSONObject json) throws JSONException {
    setUser(json.getString(USER_KEY));
    setDomain(json.getString(DOMAIN_KEY));
    setPassword(json.getString(PASSWORD_KEY));
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   * 
   * 
   */
  public AuthRequest() {}

  /**
   *  Also sets the email which is derived from the user and domain.
   *  
   *  @param domain domain name.
   */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /**
   * Also sets the email which is derived from the user and domain.
   * 
   * @param user username
   */
  public void setUser(String user) {
    this.user = user;
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


  // Generated getters and setters
  public static String getUSER_KEY() {
    return USER_KEY;
  }

  public static String getDOMAIN_KEY() {
    return DOMAIN_KEY;
  }

  public static String getPASSWORD_KEY() {
    return PASSWORD_KEY;
  }

  public String getDomain() {
   return domain;
  }
  
  // Readonly.
  public String getEmail() {
    setEmail();
    return email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUser() {
    return user;
  }
}