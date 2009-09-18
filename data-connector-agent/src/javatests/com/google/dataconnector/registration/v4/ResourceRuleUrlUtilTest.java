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
package com.google.dataconnector.registration.v4;

import com.google.dataconnector.registration.v4.ResourceRuleUrlUtil.Scheme;

import junit.framework.TestCase;

/**
 * Tests for the {@link ResourceRuleUrlUtil} class.
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class ResourceRuleUrlUtilTest extends TestCase {

  private static final String TEST_HOST = "testhost";
  private static final int TEST_PORT = 1111;
  private static final String BADLY_FORMED_URL = "file this is bad url";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testGetSchemeInUrlFromHttpUrl() {
    try {
      Scheme scheme = new ResourceRuleUrlUtil().getSchemeInUrl(constructUrl(Scheme.HTTP,
          TEST_HOST, TEST_PORT));
      assertTrue(scheme == Scheme.HTTP);
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetSchemeInUrlFromHttpsUrl() {
    try {
      Scheme scheme = new ResourceRuleUrlUtil().getSchemeInUrl(constructUrl(Scheme.HTTPS,
          TEST_HOST, TEST_PORT));
      assertTrue(scheme == Scheme.HTTPS);
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetSchemeInUrlFromSocketUrl() {
    try {
      Scheme scheme = new ResourceRuleUrlUtil().getSchemeInUrl(constructUrl(Scheme.SOCKET,
          TEST_HOST, TEST_PORT));
      assertTrue(scheme == Scheme.SOCKET);
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetSchemeInUrlNotOneOfTheAllowedValues() {
    try {
      Scheme scheme = new ResourceRuleUrlUtil().getSchemeInUrl("file://blah:1111");
      fail("exception expected");
    } catch (ResourceUrlException e) {
      assertTrue(e.getMessage().contains("resource url can only start with"));
    }
  }

  public void testGetSchemeInUrlFromBadlyFormedUrl() {
    try {
      Scheme scheme = new ResourceRuleUrlUtil().getSchemeInUrl(BADLY_FORMED_URL);
      fail("exception expected");
    } catch (ResourceUrlException e) {
      assertTrue(e.getMessage().contains("badly formed resource url"));
    }
  }

  public void testGetHostNameFromHttpUrl() {
    try {
      String host = new ResourceRuleUrlUtil().getHostnameFromRule(constructUrl(Scheme.HTTP,
          TEST_HOST, TEST_PORT));
      assertTrue(host.equals(TEST_HOST));
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetHostNameFromHttpsUrl() {
    try {
      String host = new ResourceRuleUrlUtil().getHostnameFromRule(constructUrl(Scheme.HTTPS,
          TEST_HOST, TEST_PORT));
      assertTrue(host.equals(TEST_HOST));
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetHostNameFromSocketUrl() {
    try {
      String host = new ResourceRuleUrlUtil().getHostnameFromRule(constructUrl(Scheme.SOCKET,
          TEST_HOST, TEST_PORT));
      assertTrue(host.equals(TEST_HOST));
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetHostNameFromBadlyFormedUrl() {
    try {
      String host = new ResourceRuleUrlUtil().getHostnameFromRule(BADLY_FORMED_URL);
      fail("exception expected");
    } catch (ResourceUrlException e) {
      assertTrue(e.getMessage().contains("badly formed resource url"));
    }
  }

  public void testGetPortFromHttpUrl() {
    try {
      int port = new ResourceRuleUrlUtil().getPortFromRule(constructUrl(Scheme.HTTP,
          TEST_HOST, TEST_PORT));
      assertTrue(port == TEST_PORT);
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetPortFromHttpsUrl() {
    try {
      int port = new ResourceRuleUrlUtil().getPortFromRule(constructUrl(Scheme.HTTPS,
          TEST_HOST, TEST_PORT));
      assertTrue(port == TEST_PORT);
    } catch (ResourceUrlException e) {
      fail("unexpected exception " + e.getMessage());
    }
  }

  public void testGetPortFromBadlyFormedUrl() {
    try {
      int port = new ResourceRuleUrlUtil().getPortFromRule(BADLY_FORMED_URL);
      fail("exception expected");
    } catch (ResourceUrlException e) {
      assertTrue(e.getMessage().contains("badly formed resource url"));
    }
  }

  private String constructUrl(Scheme scheme, String host, int port) {
    return scheme.name() + "://" + host + ":" + port;
  }
}
