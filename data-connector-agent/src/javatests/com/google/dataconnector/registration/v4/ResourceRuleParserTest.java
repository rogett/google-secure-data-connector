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
 *
 * $Id$
 */
package com.google.dataconnector.registration.v4;

import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.RegistrationException;

import junit.framework.TestCase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

/**
 * Tests for the {@link ResourceRuleParser} class.
 *
 * This is a "LargeTest" as it writes files.
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class ResourceRuleParserTest extends TestCase {

  private static final String TEST_AGENTID = "it_is_I";
  private static final String TEST_AGENTID_ALL = "all";
  private static final String TEST_AGENTID_NOTME = "someone_else";

  private static final String HTTP_PATTERN = "http://www.example.com";
  private static final String SOCKET_PATTERN = "socket://128.195.131";
  private static final String URL_EXACT_PATTERN = "http://www.example.com/exact/path";
  private static final String HTTPS_PATTERN = "https://www.example.com";
  private static String TEST_FILE_NAME = "/tmp/resources.xml." + System.currentTimeMillis();

  private FileUtil fileUtil = new FileUtil();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testParseResourcesFile() throws RegistrationException, XMLStreamException,
      FactoryConfigurationError, IOException {
    // the following has 4 resources defined. all of them for this agent.
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + HTTP_PATTERN + "</url>" +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + SOCKET_PATTERN + "</url> " +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_ALL + "</agentId> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + URL_EXACT_PATTERN + "</url> " +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_ALL + "</agentId> " +
        "  <url>" + HTTPS_PATTERN + "</url> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);

    // do we have 4 urls returned
    assertEquals(4, urlList.size());
  }

  public void testParseResourcesFileWithRulesForOtherAgents()
      throws RegistrationException, XMLStreamException,
      FactoryConfigurationError, IOException {
    // the following has 5 resources defined. - with one rule belonging to a different agent
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + HTTP_PATTERN + "</url>" +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + SOCKET_PATTERN + "</url> " +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_ALL + "</agentId> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + URL_EXACT_PATTERN + "</url> " +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_ALL + "</agentId> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + URL_EXACT_PATTERN + "</url> " +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_NOTME + "</agentId> " +
        "  <url>" + HTTPS_PATTERN + "</url> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);

    // do we have 4 urls returned
    assertEquals(4, urlList.size());
  }

  public void testParseResourcesFileWithNORulesForThisAgent()
      throws RegistrationException, XMLStreamException,
      FactoryConfigurationError, IOException {
    // the following has 2 resources defined - none of them belonging to this agent
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <agentId>" + TEST_AGENTID_NOTME + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + HTTP_PATTERN + "</url>" +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID_NOTME + "</agentId> " +
        "  <url>" + HTTPS_PATTERN + "</url> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);

    // do we have 0 urls returned
    assertEquals(0, urlList.size());
  }

  public void testParseResourcesFileWithNoAgentidFor1Resource()
      throws XMLStreamException,
      FactoryConfigurationError, IOException {
    // the following has 2 resources defined - one of them doesn't have agentId defined
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + HTTP_PATTERN + "</url>" +
        "</rule> " +
        "<rule> " +
        "  <url>" + HTTPS_PATTERN + "</url> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList;
    try {
      urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);
      fail("RegistrationException expected to be thrown");
    } catch (RegistrationException e) {
      assertTrue(e.getMessage().contains("resources.xml file is mising url / agentId"));
    }
  }

  public void testParseResourcesFileWithNoUrlFor1Resource()
      throws XMLStreamException,
      FactoryConfigurationError, IOException {
    // the following has 2 resources defined - one of them doesn't have agentId defined
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <url>" + HTTP_PATTERN + "</url>" +
        "</rule> " +
        "<rule> " +
        "  <agentId>" + TEST_AGENTID + "</agentId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList;
    try {
      urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);
      fail("RegistrationException expected to be thrown");
    } catch (RegistrationException e) {
      assertTrue(e.getMessage().contains("resources.xml file is mising url / agentId"));
    }
  }

  public void testParseResourcesFileNonexistentFile()
      throws XMLStreamException, FactoryConfigurationError, IOException, RegistrationException {
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList;
    try {
      urlList = resourceRuleParser.parseResourcesFile("nosuchfile", TEST_AGENTID);
      fail("RegistrationException expected to be thrown");
    } catch (FileNotFoundException e) {
      // expected
    }
  }

  public void testParseResourcesFile_allowsDeprecatedTags() throws RegistrationException,
      XMLStreamException, FactoryConfigurationError, IOException {
    // the following has 4 resources defined. all of them for this agent.
    String resourceXml = "<resourceRules> " +
        "<rule repeatable=\"true\"> " +
        "  <clientId>" + TEST_AGENTID + "</clientId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <pattern>" + HTTP_PATTERN + "</pattern>" +
        "</rule> " +
        "<rule> " +
        "  <clientId>" + TEST_AGENTID + "</clientId>" +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <pattern>" + SOCKET_PATTERN + "</pattern> " +
        "</rule> " +
        "<rule> " +
        "  <clientId>" + TEST_AGENTID_ALL + "</clientId> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "  <pattern>" + URL_EXACT_PATTERN + "</pattern> " +
        "</rule> " +
        "<rule> " +
        "  <clientId>" + TEST_AGENTID_ALL + "</clientId> " +
        "  <pattern>" + HTTPS_PATTERN + "</pattern> " +
        "  <blah>foobar</blah>" +
        "  <yadiyadayada>doesnt-matter</yadiyadayada>" +
        "</rule> " +
        "</resourceRules> " ;
    fileUtil.writeFile(TEST_FILE_NAME, resourceXml);

    // test
    ResourceRuleParser resourceRuleParser = new ResourceRuleParser(fileUtil);
    List<String> urlList = resourceRuleParser.parseResourcesFile(TEST_FILE_NAME, TEST_AGENTID);

    // do we have 4 urls returned
    assertEquals(4, urlList.size());
  }
}
