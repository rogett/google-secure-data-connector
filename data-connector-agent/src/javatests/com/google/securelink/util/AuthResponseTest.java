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
package com.google.securelink.util;

import com.google.securelink.util.AuthResponse;

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
