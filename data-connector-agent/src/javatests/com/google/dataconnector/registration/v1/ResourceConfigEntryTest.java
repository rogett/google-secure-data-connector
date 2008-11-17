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
import com.google.dataconnector.registration.v1.SocketResourceConfigEntry;

import junit.framework.TestCase;

import org.json.JSONObject;

/**
 * Tests for the {@link ResourceConfigEntry} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceConfigEntryTest extends TestCase {

  private static final Long VALID_KEY = 1L;
  private static final int VALID_SEQNUM = 1;
  
  private static final String TEST_SOCKET_PATTERN = "socket://foo.com:3999";
  private static final String TEST_ALLOWED_ENTITIES = "user1,group1";
  
  public void testGetSecurityKey() {
    
    // Because the class is abstract, stub out all abstract methods and test the implemented ones.
    ResourceConfigEntry entry = new ResourceConfigEntry(VALID_KEY, VALID_SEQNUM) {
      @Override
      public String getPattern() { return "";}
      @Override
      public boolean isAuthorized(Long key, String pattern) { return true;}
      @Override
      public void setPattern(String pattern) {}
      @Override
      public Integer getPort() { return 0;}
      @Override
      public JSONObject toJSON() { return new JSONObject(); }
    };
    assertEquals(entry.getSecurityKey(), VALID_KEY);
  }
  
  public void testEquals() throws ResourceException {
    // Valid case.  
    ResourceConfigEntry entry = 
        new SocketResourceConfigEntry(VALID_KEY, TEST_SOCKET_PATTERN, TEST_ALLOWED_ENTITIES, 
            VALID_SEQNUM);
    ResourceConfigEntry entry2 = 
        new SocketResourceConfigEntry(VALID_KEY, TEST_SOCKET_PATTERN, TEST_ALLOWED_ENTITIES,
            VALID_SEQNUM);
    assertTrue(entry.equals(entry2));
    
    // Invalid case
    entry2.setPattern("socket://SOMEDIFFERENTPATTERN:3333");
    assertFalse(entry.equals(entry2));
  }
  
  public void testSetAllowedEntities() throws ResourceException {
    // create en try to test with  
    ResourceConfigEntry entry = 
      new SocketResourceConfigEntry(VALID_KEY, TEST_SOCKET_PATTERN, TEST_ALLOWED_ENTITIES,
          VALID_SEQNUM);
    
    // are the entities sorted
    assertEquals("group1,user1", entry.getAllowedEntitiesAsString());
    
    // test invalid case
    try {
      entry.setAllowedEntities("   ");
    } catch (ResourceException e) {
      assertTrue(e.getMessage().startsWith("Invalid value for AllowedEntities"));
    }
  }
}
