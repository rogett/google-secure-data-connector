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

/**
 * First class object representing of AuthResponse for Secure Link protocol.  This provides a 
 * toJson() utility to serialize information for over-the-wire communication and provides
 * a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AuthResponse {
  
  // Constants for auth status.
  public enum Status {
    OK,
    ACCESS_DENIED;
  }
  
  private static final String STATUS_KEY = "status";
  
  private Status status;
  
  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(STATUS_KEY, status.toString());
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing.
   */
  public AuthResponse(final JSONObject json) throws JSONException {
    try {
      this.status = Status.valueOf(json.getString(STATUS_KEY));
    } catch (IllegalArgumentException e) {
      throw new JSONException("Invalid status entry: " + json.getString(STATUS_KEY));
    }
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   */
  public AuthResponse() {}

  // Getters and setters
  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public static String getSTATUS_KEY() {
    return STATUS_KEY;
  }
}