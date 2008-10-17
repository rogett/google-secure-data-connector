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

import com.google.dataconnector.util.ResourceConfigEntry;
import com.google.dataconnector.util.ResourceConfigException;
import com.google.dataconnector.util.SocketResourceConfigEntry;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link SocketResourceConfigEntry} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SocketResourceConfigEntryTest extends TestCase {

  private SocketResourceConfigEntry entry;
  private static final String TEST_HOST = "128.195.131.10";
  private static final String TEST_PORT = "143";
  private static final String TEST_WORKING_PATTERN = "socket://" +TEST_HOST + ":" + TEST_PORT;
  private static final Long TEST_KEY = 1L;
  private static final String TEST_ALLOWED_ENTRIES = "user1,group1";
  private static final String TEST_ALLOWED_ENTRIES_SORTED = "group1,user1";
  private static final int VALID_SEQNUM = 1;

  
  /**
   * Configures a {@link SocketResourceConfigEntry} with test patterns.
   */
  @Override
  protected void setUp() throws ResourceConfigException {
    entry = new SocketResourceConfigEntry(TEST_KEY, TEST_WORKING_PATTERN, TEST_ALLOWED_ENTRIES,
        VALID_SEQNUM);
  }
  
  public void testCreateFromPattern() throws ResourceConfigException {
    // Successful case.  Will throw a ResourceConfigException if invalid.
    assertEquals(entry.getPattern(), TEST_WORKING_PATTERN);
    assertEquals(entry.getSecurityKey(), TEST_KEY);
    entry = new SocketResourceConfigEntry(TEST_WORKING_PATTERN, TEST_ALLOWED_ENTRIES,
        VALID_SEQNUM);
    assertNotNull(entry.securityKey);
  }
  
  public void testIsAuthorized() {
    assertTrue(entry.isAuthorized(TEST_KEY, TEST_WORKING_PATTERN));
    assertFalse(entry.isAuthorized(TEST_KEY, "0.0.0.0:343"));
    assertFalse(entry.isAuthorized(TEST_KEY, "BADPATTERN$$$&#"));
  }
  
  public void testCreateFromJsonAndToJson() throws JSONException, ResourceConfigException {

    // Setup test JSON object
    JSONObject testJson = entry.toJSON();
    testJson.put(SocketResourceConfigEntry.JSON_TYPE_KEY, SocketResourceConfigEntry.class.getName());
    testJson.put(SocketResourceConfigEntry.JSON_SECURITY_KEY, TEST_KEY);
    testJson.put(SocketResourceConfigEntry.JSON_PATTERN_KEY, TEST_WORKING_PATTERN);
    testJson.put(ResourceConfigEntry.JSON_ALLOWEDENTITIES_KEY, TEST_ALLOWED_ENTRIES);
    
    // Populate entry with test JSON.
    entry = new SocketResourceConfigEntry(testJson);
    
    // Get JSON from object and verify.
    JSONObject entryJson = entry.toJSON();
    assertEquals(entryJson.get(SocketResourceConfigEntry.JSON_TYPE_KEY), 
        SocketResourceConfigEntry.class.getName());
    assertEquals(entryJson.get(SocketResourceConfigEntry.JSON_SECURITY_KEY), TEST_KEY);
    assertEquals(entryJson.get(SocketResourceConfigEntry.JSON_PATTERN_KEY), TEST_WORKING_PATTERN);
    assertEquals(entryJson.get(ResourceConfigEntry.JSON_ALLOWEDENTITIES_KEY), 
        TEST_ALLOWED_ENTRIES_SORTED);
  }
  
  /**
   * Tests {@link SocketResourceConfigEntry#setPattern(String)} with one good pattern and a varied 
   * set of bad patterns.
   */
  public void testSetPatten() throws ResourceConfigException {
    
    // Successful cases.  Will throw ResourceConfigException if it fails.
    entry.setPattern(TEST_WORKING_PATTERN);
    entry.setPattern("socket://a-b_c.corp.google-corp_local_confidential-host.com:80");

    // Bad host pattern tests
    String pattern = "asdfasdfasf$#$@#@#$#:$#$$$$$$$$$$$$$$$$$$r";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Not a socket pattern"));
    }
    pattern = "socket://asdfasdfasf$#$@#@#$#:$#$$$$$$$$$$$$$$$$$$";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Invalid host given"));
    }
    pattern = "socket://a.b.:80";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Invalid host given"));
    }
    pattern = "socket://foo.corp.example.com:324324324";
    try {

      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Port out of range"));
    }
    pattern = "socket://foo.corp.example.com:notaport";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Invalid port"));
    }
    pattern = "socket://foo.corp.example.com";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Invalid socket pattern"));
    }
    pattern = "socket://:342342";
    try {
      entry.setPattern(pattern);
    } catch (ResourceConfigException e) {
      assertTrue(e.getMessage().startsWith("Invalid socket pattern"));
    }
  }
}
