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

import com.google.dataconnector.registration.v2.AuthResponse;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link AuthResponse} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AuthResponseTest extends TestCase {

  private JSONObject rawAuthResponseJson;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testAuthResponse() throws JSONException {
    // Success case.
    rawAuthResponseJson = new JSONObject();
    rawAuthResponseJson.put(AuthResponse.getSTATUS_KEY(), 
        AuthResponse.Status.OK.toString());
    AuthResponse regResponse = new AuthResponse(rawAuthResponseJson);
    assertEquals(regResponse.toJson().toString(), rawAuthResponseJson.toString());

    // Error case
    rawAuthResponseJson = new JSONObject();
    rawAuthResponseJson.put(AuthResponse.getSTATUS_KEY(), 
        AuthResponse.Status.ACCESS_DENIED);
    regResponse = new AuthResponse(rawAuthResponseJson);
    assertEquals(regResponse.toJson().toString(), rawAuthResponseJson.toString());

    // Setter
    regResponse = new AuthResponse();
    regResponse.setStatus(AuthResponse.Status.ACCESS_DENIED);
    assertEquals(regResponse.toJson().toString(), rawAuthResponseJson.toString());
  }
  
}
