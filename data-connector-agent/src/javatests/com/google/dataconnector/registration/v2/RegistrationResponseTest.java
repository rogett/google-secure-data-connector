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

import com.google.dataconnector.registration.v2.RegistrationResponse;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link RegistrationResponse} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationResponseTest extends TestCase {

  private JSONObject rawResponseJson;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testRegistrationResponse() throws JSONException {
    // Success case.
    rawResponseJson = new JSONObject();
    rawResponseJson.put(RegistrationResponse.getSTATUS_KEY(), 
        RegistrationResponse.Status.OK.toString());
    rawResponseJson.put(RegistrationResponse.getERROR_MSG_KEY(), "");
    RegistrationResponse regResponse = new RegistrationResponse(rawResponseJson);
    assertEquals(regResponse.toJson().toString(), rawResponseJson.toString());

    // Error case
    rawResponseJson = new JSONObject();
    rawResponseJson.put(RegistrationResponse.getSTATUS_KEY(), 
        RegistrationResponse.Status.REGISTRATION_ERROR);
    rawResponseJson.put(RegistrationResponse.getERROR_MSG_KEY(), "error message");
    regResponse = new RegistrationResponse(rawResponseJson);
    assertEquals(regResponse.toJson().toString(), rawResponseJson.toString());
    
    regResponse = new RegistrationResponse();
    regResponse.setStatus(RegistrationResponse.Status.REGISTRATION_ERROR);
    regResponse.setErrorMsg("error message");
    assertEquals(regResponse.toJson().toString(), rawResponseJson.toString());
  }
  
}
