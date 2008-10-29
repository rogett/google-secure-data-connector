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

import com.google.dataconnector.client.testing.FakeClientConfiguration;
import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.ConfigurationException;
import com.google.dataconnector.util.RegistrationRequest;
import com.google.dataconnector.util.ResourceConfigEntry;
import com.google.dataconnector.util.ResourceConfigException;
import com.google.dataconnector.util.SocketResourceConfigEntry;
import com.google.dataconnector.util.UriResourceConfigEntry;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link RegistrationRequest} class
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationRequestTest extends TestCase {

  private ClientConf fakeClientConf;
  private List<ResourceConfigEntry> confUriEntries;
  private List<ResourceConfigEntry> confSocketEntries;
  
  
  @Override
  protected void setUp() throws ConfigurationException {
    fakeClientConf = new FakeClientConfiguration().getFakeClientConf();
    confUriEntries = new ArrayList<ResourceConfigEntry>();
    confSocketEntries = new ArrayList<ResourceConfigEntry>();
    for (ResourceConfigEntry confEntry : fakeClientConf.getRules()) {
      if (confEntry instanceof UriResourceConfigEntry) {
        confUriEntries.add(confEntry);
      } else if (confEntry instanceof SocketResourceConfigEntry) {
        confSocketEntries.add(confEntry);
      }
    }
  }
  
  public void testToJson() throws JSONException, ResourceConfigException {
    RegistrationRequest requestJson = new RegistrationRequest(fakeClientConf);
    RegistrationRequest requestJson1 = 
        new RegistrationRequest(new JSONObject(requestJson.toJson().toString()));
    assertEquals(requestJson, requestJson1);
    JSONObject rawJson = requestJson.toJson();
    JSONArray array = rawJson.getJSONArray(RegistrationRequest.RESOURCES_KEY);
    assertEquals(rawJson.getString(RegistrationRequest.CLIENT_ID_KEY), 
        fakeClientConf.getClient_id());
    assertEquals(new Integer(rawJson.getInt(RegistrationRequest.SOCKS_PORT)),
        fakeClientConf.getSocksServerPort());
    
    assertTrue(array.length() != 0);
    
    /*
     * Goes through each resource JSON and creates a ResourceConfigEntry instance.  If
     * the JSON is malformed this will fail.  We then verify that the config is identical
     * to the configuration from FakeClientConfiguration.
     */
    int foundEntries = 0;
    for (int index=0; index < array.length() ; index++) {
      ResourceConfigEntry entry = null;
      if (array.getJSONObject(index).getString(SocketResourceConfigEntry.JSON_TYPE_KEY).equals(
          SocketResourceConfigEntry.class.getName())) {
        entry = new SocketResourceConfigEntry(array.getJSONObject(index));
      } else if (array.getJSONObject(index).getString(UriResourceConfigEntry.JSON_TYPE_KEY).equals(
          UriResourceConfigEntry.class.getName())) {
        entry = new UriResourceConfigEntry(array.getJSONObject(index));
      }
      boolean found = false;
      for (ResourceConfigEntry resourceEntry : fakeClientConf.getRules()) {
        if (resourceEntry instanceof SocketResourceConfigEntry ) {
          if (entry.equals(resourceEntry)) {
            found = true;
            foundEntries++;
            break;
          }
        } else if (resourceEntry instanceof UriResourceConfigEntry ) {
          if (entry.equals(resourceEntry)) {
            found = true;
            foundEntries++;
            break;
          }
        }
      }
      assertEquals(found, true);
    }
    assertEquals(foundEntries, array.length());
  }
  
  public void testGetAllUriResources() throws JSONException, ResourceConfigException {
    RegistrationRequest requestJson = new RegistrationRequest(fakeClientConf);
    assertEquals(requestJson.getAllUriResources().size(), confUriEntries.size());  
    for (UriResourceConfigEntry requestJsonEntry : requestJson.getAllUriResources()) {
      boolean found = false;
      for (ResourceConfigEntry confEntry : confUriEntries) {
        if (requestJsonEntry.equals(confEntry)) {
          found = true;
          break;
        }
      }
      assertTrue(found == true);
    }
  }

  public void testGetAllSocketResources() throws JSONException, ResourceConfigException {
    RegistrationRequest requestJson = new RegistrationRequest(fakeClientConf);
    assertEquals(requestJson.getAllUriResources().size(), confUriEntries.size());  
    for (SocketResourceConfigEntry requestJsonEntry : requestJson.getAllSocketResources()) {
      boolean found = false;
      for (ResourceConfigEntry confEntry : confSocketEntries) {
        if (requestJsonEntry.equals(confEntry)) {
          found = true;
          break;
        }
      }
      assertTrue(found == true);
    }
  }

  
}
