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
  private ResourceRule runtimeUrlExactResourceRule;
  private ResourceRuleValidator resourceRuleValidator;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FakeResourceRuleConfig fakeResourceRuleConfig = new FakeResourceRuleConfig();
    configHttpResourceRule = fakeResourceRuleConfig.getFakeConfigResourceRules().get(0); 
    runtimeHttpResourceRule = fakeResourceRuleConfig.getFakeRuntimeResourceRules().get(0); 
    configSocketResourceRule = fakeResourceRuleConfig.getFakeConfigResourceRules().get(1); 
    runtimeSocketResourceRule = fakeResourceRuleConfig.getFakeRuntimeResourceRules().get(1); 
    runtimeUrlExactResourceRule = fakeResourceRuleConfig.getFakeRuntimeResourceRules().get(2); 
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
    runtimeHttpResourceRule.setRuleNum(1);
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
  
  public void testAllowDomainFlag() throws ResourceException {
    runtimeHttpResourceRule.setAllowedEntities(null);
    runtimeHttpResourceRule.setAllowDomainViewers(true);
    resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
  }

  // AppIds
  public void testBadAppId() {
    
    AppTag[] appIds = { createApp("has a space", ".*") };   
    runtimeHttpResourceRule.setApps(appIds);
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("must not contain any white space"));
      return;
    }
    fail("did not get ResourceException");
  }
  
  public void testMissingAppIds() {
    runtimeHttpResourceRule.setApps(null);
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("field must be present"));
      return;
    }
    fail("did not get ResourceException");
  }
 
  public void testAllowAllAppsFlag() throws ResourceException {
    AppTag app = createApp("container", null);
    app.setAllowAnyAppId(true);
    AppTag[] apps = { app };
    runtimeHttpResourceRule.setApps(apps);
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
  
  public void testUrlPathInHostPortPattern() {
    runtimeHttpResourceRule.setPattern("http://foo.com/has/path");
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("cannot contain any path"));
      return;
    }
  }
  
  public void testUrlExactTypeCannotBeUsedWithHttpsPattern() {
    runtimeHttpResourceRule.setPatternType(ResourceRule.URLEXACT);
    runtimeHttpResourceRule.setPattern("https://somehttpssite.com");
    try {
      resourceRuleValidator.validateRuntime(runtimeHttpResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("URLEXACT works only"));
      return;
    }
  }
  
  public void testUrlExactTypeCannotBeUsedWithSocketPattern() {
    runtimeSocketResourceRule.setPatternType(ResourceRule.URLEXACT);
    try {
      resourceRuleValidator.validateRuntime(runtimeSocketResourceRule);
    } catch (ResourceException e) {
      assertTrue(e.getMessage().contains("URLEXACT works only"));
      return;
    }
  }

  private AppTag createApp(String container, String appId) {
    AppTag app = new AppTag();
    app.setContainer(container);
    app.setAppId(appId);
    return app;
  }
}
