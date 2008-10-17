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

import com.google.dataconnector.util.RegistrationResponse;

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
