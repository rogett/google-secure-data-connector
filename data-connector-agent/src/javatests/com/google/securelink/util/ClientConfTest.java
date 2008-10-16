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

import com.google.securelink.util.ClientConf;
import com.google.securelink.util.ConfigurationException;
import com.google.securelink.util.ResourceConfigEntry;

import junit.framework.TestCase;

import org.apache.commons.cli.CommandLine;
import org.easymock.classextension.EasyMock;

import java.util.List;
import java.util.Properties;

/**
 * Tests for the {@link ClientConf} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientConfTest extends TestCase {

  private Properties testProps;
  private CommandLine flags;
  
  @Override
  protected void setUp() {
    testProps = new Properties();
    testProps.setProperty("stringTest", "stringValue");
    testProps.setProperty("integerTest", "1343");
    ClientConf.setFlagsForTest(EasyMock.createMock(CommandLine.class));
    flags = ClientConf.getFlagsForTest();
    EasyMock.expect(flags.getOptionValue("stringTest", "stringValue")).andReturn(
        "stringValue").anyTimes();
    EasyMock.expect(flags.getOptionValue("integerTest", "1343")).andReturn("1343").anyTimes();
    EasyMock.expect(flags.getOptionValue("NONEXISTENTKEY", null)).andReturn(null).anyTimes();
    EasyMock.replay(flags);
  }
  
  public void testProcessResourceEntries() throws ConfigurationException {

    Properties props = new Properties();
    String VALID_SOCKET_PATTERN = "socket://128.195.131.10:389";
    String VALID_HTTP_PATTERN = "http://www.kuci.org/\\.*";
    String VALID_HTTPS_PATTERN = "https://www.kuci.org/\\.*";
    String ALLOWED_ENTITIES = "user1,group1";

    // SOCKET pattern
    props.setProperty("resource1", VALID_SOCKET_PATTERN + " " + ALLOWED_ENTITIES);
    List<ResourceConfigEntry> resources = ClientConf.processResourceEntries(props, 1024);
    assertEquals(resources.get(0).getPattern(), VALID_SOCKET_PATTERN);

    // HTTP pattern
    props.setProperty("resource1", VALID_HTTP_PATTERN + " " + ALLOWED_ENTITIES);
    resources = ClientConf.processResourceEntries(props, 1024);
    assertEquals(resources.get(0).getPattern(), VALID_HTTP_PATTERN);

    // HTTPS pattern
    props.setProperty("resource1", VALID_HTTPS_PATTERN + " " + ALLOWED_ENTITIES);
    resources = ClientConf.processResourceEntries(props, 1024);
    assertEquals(resources.get(0).getPattern(), VALID_HTTPS_PATTERN);
    
    /* Bad pattern, we will only do one since each ResourceConfigEntry has its own tests
     * for various bad patterns
     */
    props.setProperty("resource1", "sdafasfd://asdfasf/asdfasf");
    try {
      resources = ClientConf.processResourceEntries(props, 1024);
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().startsWith("Invalid firewall rule"));
    }
    
  }
  
  
  public void testGetAndCheckProperty() throws ConfigurationException {

    // Check "required" version.  Will throw ConfigurationException if broken.
    ClientConf.getAndCheckProperty(testProps, "stringTest"); 
    try {
      ClientConf.getAndCheckProperty(testProps, "NONEXISTENTKEY");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().startsWith("Missing configuration entry"));
    }
    
    // Check defaultValue is returned for nonexistent keys
    assertEquals("defaultValue", 
        ClientConf.getAndCheckProperty(testProps, "NONEXISTENTKEY", "defaultValue"));
    
    // Check defaultValue is not returned for keys that exist.
    assertEquals("stringValue", 
        ClientConf.getAndCheckProperty(testProps, "stringTest", "defaultValue"));
  }
  
  public void testGetAndCheckIntegerProperty() throws ConfigurationException {

    // Check "required" version.  Will throw ConfigurationException if broken.
    int flagValue = ClientConf.getAndCheckIntegerProperty(testProps, "integerTest");
    assertEquals(flagValue, 1343);
    
    // Check non integer key
    try {
      ClientConf.getAndCheckIntegerProperty(testProps, "stringTest");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().startsWith("Invalid numeric property"));
    }
    
    // Check non existent key.
    try {
      ClientConf.getAndCheckIntegerProperty(testProps, "NONEXISTENTKEY");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().startsWith("Missing configuration entry"));
    }
  }
  
}
