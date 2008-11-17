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

import com.google.dataconnector.registration.v1.ResourceConfigEntry;
import com.google.dataconnector.registration.v1.UriResourceConfigEntry;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for the {@link UriResourceConfigEntry} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class UriResourceConfigEntryTest extends TestCase {

  private UriResourceConfigEntry entry;
  private static final Integer TEST_PROXY_PORT = 3128;
  private static final String VALID_PATTERN = "http://kuci.org/.*";
  private static final String AUTHORIZED_URL = "http://kuci.org/asdfasdfas";
  private static final String NOT_AUTHORIZED_URL = "http://www.example.com";
  private static final String INVALID_URL = "htp:a324324asdfabaasdf///asdfs../";
  private static final Long VALID_KEY = 1L;
  private static final Long INVALID_KEY = 32423432L;
  private static final String TEST_ALLOWED_ENTRIES = "user1,group1";
  private static final String TEST_ALLOWED_ENTRIES_SORTED = "group1,user1";
  private static final int VALID_SEQNUM = 1;

  @Override
  protected void setUp() throws ResourceException {
    entry = new UriResourceConfigEntry(VALID_KEY, VALID_PATTERN, 
        TEST_ALLOWED_ENTRIES, TEST_PROXY_PORT, VALID_SEQNUM);
  }
  
  public void testConstructor() throws ResourceException {
    // Successful case.  Will throw a ResourceConfigException if invalid.
    assertEquals(entry.getSecurityKey(), VALID_KEY);
    assertEquals(entry.getPattern(), VALID_PATTERN);
    assertEquals(entry.getPort(), TEST_PROXY_PORT);
    entry = new UriResourceConfigEntry(VALID_PATTERN, TEST_ALLOWED_ENTRIES, TEST_PROXY_PORT,
        VALID_SEQNUM);
    assertNotNull(entry.securityKey);
  }
  
  public void testIsAuthorized() {
    assertTrue(entry.isAuthorized(VALID_KEY, AUTHORIZED_URL));
    assertFalse(entry.isAuthorized(VALID_KEY, NOT_AUTHORIZED_URL));
    assertFalse(entry.isAuthorized(INVALID_KEY, AUTHORIZED_URL));
    assertFalse(entry.isAuthorized(VALID_KEY, INVALID_URL));
  }
  
  public void testCreateFromJsonAndToJson() throws JSONException, ResourceException {
    // Setup test JSON with fake values.
    JSONObject testJson = entry.toJSON();
    testJson.put(UriResourceConfigEntry.JSON_TYPE_KEY, UriResourceConfigEntry.class.getName());
    testJson.put(UriResourceConfigEntry.JSON_SECURITY_KEY, VALID_KEY);
    testJson.put(UriResourceConfigEntry.JSON_URI_PATTERN_KEY, VALID_PATTERN);
    testJson.put(UriResourceConfigEntry.JSON_PROXY_PORT_KEY, TEST_PROXY_PORT);
    testJson.put(ResourceConfigEntry.JSON_ALLOWEDENTITIES_KEY, TEST_ALLOWED_ENTRIES);
    
    // Create from test JSON.
    entry = new UriResourceConfigEntry(testJson);
    
    // Call toJSON() and compare values with testJSON.
    JSONObject entryJson = entry.toJSON();
    assertEquals(entryJson.get(UriResourceConfigEntry.JSON_TYPE_KEY), 
        UriResourceConfigEntry.class.getName());
    assertEquals(entryJson.get(UriResourceConfigEntry.JSON_SECURITY_KEY), VALID_KEY);
    assertEquals(entryJson.get(UriResourceConfigEntry.JSON_URI_PATTERN_KEY), VALID_PATTERN);
    assertEquals(entryJson.get(UriResourceConfigEntry.JSON_PROXY_PORT_KEY), TEST_PROXY_PORT);
    assertEquals(entryJson.get(ResourceConfigEntry.JSON_ALLOWEDENTITIES_KEY), 
        TEST_ALLOWED_ENTRIES_SORTED);
  }
  
  /**
   * Verifies {@link UriResourceConfigEntry#setPattern(String)} with good and bad URL patterns
   */
  public void testSetPatten() throws ResourceException {
    
    // Successful case.  Will throw ResourceConfigException if it fails.
    entry.setPattern(VALID_PATTERN);

    // Bad host pattern tests
    String pattern = "asdfasdfasf$#$@#@#$#:$#$$$$$$$$$$$$$$$$$$";
    try {
      entry.setPattern(pattern);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Not an http pattern.  Must"));
    }
    // Bad host pattern tests
    pattern = "asdfasdfasf$#$@#@#$#:$#$$$$$$$$$$$$$$$$$$ ";
    try {
      entry.setPattern(pattern);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Not an http pattern.  Must"));
    }
    pattern = "socket://foo.corp.example.com:324324324";
    try {
      entry.setPattern(pattern);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Not an http pattern.  Must"));
    }
    pattern = "socket://foo.corp.example.com:324324324";
    try {
      entry.setPattern(pattern);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Not an http pattern.  Must"));
    }
  }
}
