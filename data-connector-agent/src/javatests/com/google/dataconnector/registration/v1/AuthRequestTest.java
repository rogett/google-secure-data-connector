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

import com.google.dataconnector.registration.v1.AuthRequest;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link AuthRequest} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AuthRequestTest extends TestCase {

  private static final String TEST_USER = "boo";
  private static final String TEST_DOMAIN = "joonix.net";
  private static final String TEST_PASSWORD = "password";
  
  private JSONObject rawAuthJson;
  
  /**
   * Sets up a raw JSONObject with the proper keys and values for further processes.
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rawAuthJson = new JSONObject();
    rawAuthJson.put(AuthRequest.USER_KEY, TEST_USER);
    rawAuthJson.put(AuthRequest.DOMAIN_KEY, TEST_DOMAIN);
    rawAuthJson.put(AuthRequest.PASSWORD_KEY, TEST_PASSWORD);
  }

  public void testConstructorAndToJson() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
    assertEquals(authJson.toJson().toString(), rawAuthJson.toString());
  }
  
  public void testGetEmail() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
    assertEquals(TEST_USER + "@" + TEST_DOMAIN, authJson.getEmail());
  }
  
}
