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

import com.google.dataconnector.registration.v2.AuthRequest;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link AuthRequest} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AuthRequestTest extends TestCase {

  
  private JSONObject rawAuthJson;
  
  /**
   * Sets up a raw JSONObject with the proper keys and values for further processes.
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rawAuthJson = new JSONObject();
    rawAuthJson.putOpt(AuthRequest.DOMAIN_KEY, "testdomain");
    rawAuthJson.putOpt(AuthRequest.USER_KEY, "testuser");
    rawAuthJson.putOpt(AuthRequest.PASSWORD_KEY, "testpassword");
  }

  public void testConstructorAndToJson() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
  }
  
  public void testGetEmail() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
  }
  
}
