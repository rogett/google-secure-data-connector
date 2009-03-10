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
 * First class object representing the Registration response for the Secure Link protocol.  This 
 * provides a toJson() utility to serialize information for over-the-wire communication and 
 * provides a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationResponse {
  
  public enum Status {
    OK,
    REGISTRATION_ERROR,
  }
  private static final String STATUS_KEY = "status";
  private static final String ERROR_MSG_KEY = "errorMsg";
  
  private Status status;
  private String errorMsg;
  
  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(STATUS_KEY, status);
    json.put(ERROR_MSG_KEY, errorMsg);
    return json;
  }
  
  /**
   * Creates object from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws JSONException if any required fields are missing or not valid for this object.
   */
  public RegistrationResponse(final JSONObject json) throws JSONException {
    try {
      status = Status.valueOf(json.getString(STATUS_KEY));
    } catch (IllegalArgumentException e) {
      throw new JSONException("Invalid status entry: " + json.getString(STATUS_KEY));
    }
    if (status != Status.OK) {
      errorMsg = json.getString(ERROR_MSG_KEY);
    } else {
      errorMsg = "";
    }
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   */
  public RegistrationResponse() {}

  public Status getStatus() {
    return status;
  }

  // Getters and setters
  public void setStatus(Status status) {
    this.status = status;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public static String getSTATUS_KEY() {
    return STATUS_KEY;
  }

  public static String getERROR_MSG_KEY() {
    return ERROR_MSG_KEY;
  }
  // End Getters and setters
}
