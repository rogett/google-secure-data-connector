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
    rawAuthJson.putOpt(AuthRequest.OAUTH_KEY, "test_oauth_key");
  }

  public void testConstructorAndToJson() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
  }
  
  public void testGetEmail() throws JSONException {
    AuthRequest authJson = new AuthRequest(rawAuthJson);
  }
  
}
