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
package com.google.dataconnector.registration.v4.testing;


/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeResourceRuleConfig {

  // Resource Values
  public static final String AGENT_ID = "all";
  public static final int HTTP_RULE_NUM = 1;
  public static final String[] ALLOWED_ENTITY = 
      { "rcolline@test.joonix.net", "admin@test.joonix.net" };
  public static final String HTTP_PATTERN = "http://www.example.com";
  public static final int SOCKET_RULE_NUM = 2;
  public static final String SOCKET_PATTERN = "socket://128.195.131";
  public static final String SOCKS_SERVER_PORT = "1080";
  public static final String SECRET_KEY = "23423432432";
  public static final int URL_EXACT_RULE_NUM = 3;
  public static final String URL_EXACT_PATTERN = "http://www.example.com/exact/path";
  public static final int HTTPS_RULE_NUM = 4;
  public static final String HTTPS_PATTERN = "https://www.example.com";
  
  public static final String CONFIG_RESOURCE_RULES_XML = "<resourceRules>\n" +
    "<rule repeatable='true'>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>" +
    "  <blah>foobar<</blah>" +
    "  <yadiyadayada>doesnt-matter<</yadiyadayada>" +
    "  <url>" + HTTP_PATTERN + "</url>" +
    "</rule>\n" +
    "<rule>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>" +
    "  <blah>foobar<</blah>" +
    "  <yadiyadayada>doesnt-matter<</yadiyadayada>" +
    "  <url>" + SOCKET_PATTERN + "</url>\n" +
    "</rule>\n" +
    "<rule>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <blah>foobar<</blah>" +
    "  <yadiyadayada>doesnt-matter<</yadiyadayada>" +
    "  <url>" + URL_EXACT_PATTERN + "</url>\n" +
    "</rule>\n" +
    "<rule>\n" +
    "  <agentId>" + AGENT_ID + "</agentId>\n" +
    "  <url>" + HTTPS_PATTERN + "</url>\n" +
    "  <blah>foobar<</blah>" +
    "  <yadiyadayada>doesnt-matter<</yadiyadayada>" +
    "</rule>\n" +
    "</resourceRules>\n" ;
  
  public static final String CONFIG_RESOURCE_RULES_XML_OLDSTYLE = "<feed>\n" +
  "<entity repeatable='true'>\n" +
  "  <clientId>" + AGENT_ID + "</clientId>\n" +
  "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
  "  <blah>foobar<</blah>" +
  "  <blah>foobar<</blah>" +
  "</entity>\n" +
  "<entity>\n" +
  "  <clientId>" + AGENT_ID + "</clientId>\n" +
  "  <pattern>" + SOCKET_PATTERN + "</pattern>\n" +
  "  <blah>foobar<</blah>" +
  "  <blah>foobar<</blah>" +
  "  <blah>foobar<</blah>" +
  "</entity>\n" +
  "<entity>\n" +
  "  <clientId>" + AGENT_ID + "</clientId>\n" +
  "  <pattern>" + URL_EXACT_PATTERN + "</pattern>\n" +
  "  <blah>foobar<</blah>" +
  "</entity>\n" +
  "<entity>\n" +
  "  <agentId>" + AGENT_ID + "</agentId>\n" +
  "  <pattern>" + HTTPS_PATTERN + "</pattern>\n" +
  "  <blah>foobar<</blah>" +
  "</entity>\n" +
  "</feed>\n" ;
  
  public static final String RUNTIME_RESOURCE_ENTITY_XML =
    "<entity repeatable='true'>\n" +
    "  <clientId>" + AGENT_ID + "</clientId>\n" +
    "  <pattern>" + HTTP_PATTERN + "</pattern>\n" +
    "  <blah>foobar<</blah>" +
    "</entity>\n";
}
