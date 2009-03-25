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

import junit.framework.TestCase;

/**
 * Tests for the {@link ResourceRuleValidator} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleValidatorTest extends TestCase {
  
  private ResourceRule runtimeHttpResourceRule;
  private ResourceRule configHttpResourceRule;
  private ResourceRule runtimeSocketResourceRule;
  private ResourceRule configSocketResourceRule;
  private ResourceRuleValidator resourceRuleValidator;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FakeResourceRuleConfig fakeResourceRuleConfig = new FakeResourceRuleConfig();
    configHttpResourceRule = fakeResourceRuleConfig.getFakeConfigResourceRules().get(0); 
    runtimeHttpResourceRule = fakeResourceRuleConfig.getFakeRuntimeResourceRules().get(0); 
    configSocketResourceRule = fakeResourceRuleConfig.getFakeConfigResourceRules().get(1); 
    runtimeSocketResourceRule = fakeResourceRuleConfig.getFakeRuntimeResourceRules().get(1); 
    resourceRuleValidator = new ResourceRuleValidator();
  }
  
  @Override
  protected void tearDown() throws Exception {
    runtimeHttpResourceRule = null;
    configHttpResourceRule = null;
    runtimeSocketResourceRule = null;
    configSocketResourceRule = null;
    super.tearDown();
  }
  
  public void testProperRuntimeResourceRules() throws ResourceException {
    resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    resourceRuleValidator.validateRuntime(runtimeSocketResourceRule);
  }
  
  public void testProperConfigResourceRules() throws ResourceException {
    resourceRuleValidator.validate(configHttpResourceRule);
    resourceRuleValidator.validate(configSocketResourceRule);
  }
  
  // Http Proxy Port
  public void testBadHttpProxyPort() {
    runtimeHttpResourceRule.setHttpProxyPort(3242343);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("out of range"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingHttpProxyPort() {
    runtimeHttpResourceRule.setHttpProxyPort(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("required for each"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  // Socks Server Port
  public void testBadSocksServerPort() {
    runtimeHttpResourceRule.setSocksServerPort(3242343);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("out of range"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingSocksServerPort() {
    runtimeHttpResourceRule.setSocksServerPort(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("required for each"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  // Secret Key
  public void testMissingSecretKey() {
    runtimeHttpResourceRule.setSecretKey(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("Rule is missing secret key"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testRuleNumSetAsZero() {
    runtimeHttpResourceRule.setRuleNum(0);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("greater than 0"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  // Client ID
  public void testBadClientId() {
    runtimeHttpResourceRule.setClientId("has a space");
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("contain any white space"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingClientId() {
    runtimeHttpResourceRule.setClientId(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("must be present"));
      return;
    }
    fail("did not get ResourceException");
  }

  // Allowed Entities
  public void testBadAllowedEntity() {
    
    String[] allowedEntitiesNoAt = { "foo" };   
    runtimeHttpResourceRule.setAllowedEntities(allowedEntitiesNoAt);
    
    boolean testfail = true;
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("fully qualified email address"));
      testfail = false;
    }
    
    // Reset test.
    runtimeHttpResourceRule.setName(null);
    String[] allowedEntitiesSpace = { "has a space" };   
    runtimeHttpResourceRule.setAllowedEntities(allowedEntitiesSpace);
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("must not contain any white space"));
      testfail = false;
    }
    assertFalse(testfail);
  }
  
  public void testMissingAllowedEntities() {
    runtimeHttpResourceRule.setAllowedEntities(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("at least one"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  // AppIds
  public void testBadAppId() {
    
    String[] appIds = { "has a space" };   
    runtimeHttpResourceRule.setAppIds(appIds);
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("must not contain any white space"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingAppIds() throws ResourceException {
    runtimeHttpResourceRule.setAppIds(null);
    resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
  }
  
  public void testBadPatternIdentifier() {
    runtimeHttpResourceRule.setPattern("asdfasdf://sdafasfd");
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("Invalid pattern"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testBadPatternHasSpace() {
    runtimeHttpResourceRule.setPattern("socket://aasdf :3233");
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("contain any white space"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingPattern() {
    runtimeHttpResourceRule.setPattern(null);
    
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("must be present"));
      return;
    }
    fail("did not get ResourceException");
  }
}
