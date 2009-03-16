/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.google.dataconnector.registration.v2;

import com.google.dataconnector.registration.v2.ResourceRule.AppTag;
import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for the {@link ResourceRuleUtil} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResoureRuleUtilTest extends TestCase {

  private FakeResourceRuleConfig fakeResourceRuleConfig;
  ResourceRuleUtil resourceRuleUtil;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil());
    fakeResourceRuleConfig = new FakeResourceRuleConfig();
  }
  
  @Override
  protected void tearDown() throws Exception {
    fakeResourceRuleConfig = null;
    resourceRuleUtil = null;
    super.tearDown();
  }
  
  public void testSetSecretKeys() {
    List<ResourceRule> resourceRules = fakeResourceRuleConfig.getFakeConfigResourceRules();
    resourceRuleUtil.setSecretKeys(resourceRules);
    for (ResourceRule resourceRule : resourceRules) {
      assertNotNull(resourceRule.getSecretKey());
    }
  }
  
  public void testGetResourceRules() throws ResourceException {
    List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
        FakeResourceRuleConfig.RUNTIME_RESOURCE_RULES_XML);
    
    // Check Http Runtime - rule 0
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    ResourceRule actual = resourceRules.get(0);
    assertEquals(expected.getRuleNum(), actual.getRuleNum());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
    
    // Socket Runtime - rule 1
    expected = fakeResourceRuleConfig.getRuntimeSocketResourceRule();
    actual = resourceRules.get(1);
    assertEquals(expected.getRuleNum(), actual.getRuleNum());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertNull(expected.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
    
    // URLEXACT Runtime - rule 2
    expected = fakeResourceRuleConfig.getRuntimeUrlExactResourceRule();
    actual = resourceRules.get(2);
    assertEquals(expected.getRuleNum(), actual.getRuleNum());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
  }
  
  public void testGetResourceRuleFromEntityXml() throws ResourceException {
    ResourceRule actual = resourceRuleUtil.getResourceRuleFromEntityXml(
        FakeResourceRuleConfig.RUNTIME_RESOURCE_ENTITY_XML);
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    assertEquals(expected.getRuleNum(), actual.getRuleNum());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
  }
  
  public void testGetEntityXmlFromResourceRule() throws ResourceException {
    // For this test, we assume that getEntityXmlFromResourceRule works as its tested above. We
    // convert from a known good ResourceRule to XML then back again.  if they are equal this code
    // probably works :)
    ResourceRule actual = resourceRuleUtil.getResourceRuleFromEntityXml(
        resourceRuleUtil.getEntityXmlFromResourceRule(
            fakeResourceRuleConfig.getRuntimeHttpResourceRule()));
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    assertEquals(expected.getRuleNum(), actual.getRuleNum());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
  }
  
  /**
   * Looks at common parameters between two resource rules.  HTTP and Socket rules always share
   * these parameters.
   * 
   * @param expected ResourceRule
   * @param actual ResourceRule
   */
  private void verifyCommonResourceParams(ResourceRule expected, ResourceRule actual) {
    assertEquals(expected.getClientId(), actual.getClientId());
    for (int index=0 ; index < expected.getAllowedEntities().length; index++) {
	  assertEquals(expected.getAllowedEntities()[index], actual.getAllowedEntities()[index]);
    }
    if (expected.getApps() != null) {
	  for (int index=0 ; index < expected.getApps().length; index++) {
	    assertNotNullAndEqual(expected.getApps()[index], actual.getApps()[index]);
	  }
    }
    assertEquals(expected.getSocksServerPort(), actual.getSocksServerPort());
    assertEquals(expected.getSecretKey(), actual.getSecretKey());
  }
  
  private void assertNotNullAndEqual(AppTag app1, AppTag app2) {
    assertNotNull(app1);
    assertNotNull(app2);
    assertEquals(app1.getContainer(), app2.getContainer());
    assertEquals(app1.getAppId(), app2.getAppId());
  }
}
