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

import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link RegistrationRequest}
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationRequestTest extends TestCase {

  private JSONObject registrationJson;
  private FakeResourceRuleConfig fakeResourceRuleConfig;
  private ResourceRuleUtil resourceRuleUtil;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fakeResourceRuleConfig = new FakeResourceRuleConfig();
    registrationJson = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    jsonArray.put(FakeResourceRuleConfig.RUNTIME_RESOURCE_ENTITY_XML);
    registrationJson.put("resources", jsonArray);
    // We don't need to provide socket and inet address because ResourceRequest does not call 
    // RegistrationUtil#getVirtualHostBindPortsAndSetHttpProxyPorts
    resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil(), null, null);
  }
  
  public void testPopulateFromJSON() throws ResourceException {
    RegistrationRequest registrationRequest = new RegistrationRequest(resourceRuleUtil);
    registrationRequest.populateFromJSON(registrationJson);
    ResourceRule actual = registrationRequest.getResources().get(0);
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    verify(expected, actual);
  }
    
  public void testPopulateFromResources() throws ResourceException {
    RegistrationRequest registrationRequest = new RegistrationRequest(resourceRuleUtil);
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    List<ResourceRule> resources = new ArrayList<ResourceRule>();
    resources.add(expected);
    registrationRequest.populateFromResources(resources);
    ResourceRule actual = registrationRequest.getResources().get(0);
    verify(expected, actual);
  }
  
  private void verify(ResourceRule expected, ResourceRule actual) {
    // Check resource rule 
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getClientId(), actual.getClientId());
    for (int index=0 ; index < expected.getAllowedEntities().length; index++) {
      assertEquals(expected.getAllowedEntities()[index], actual.getAllowedEntities()[index]);
    }
    if (expected.getAppIds() != null) {
      for (int index=0 ; index < expected.getAppIds().length; index++) {
        assertEquals(expected.getAppIds()[index], actual.getAppIds()[index]);
      }
    }
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    assertEquals(expected.getSocksServerPort(), actual.getSocksServerPort());
    assertEquals(expected.getSecretKey(), actual.getSecretKey());
  }
}
