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
import com.google.inject.Inject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * parses the resource rules xml file to extract just what the agent needs; i.e., it doesn't
 * parse the whole xml file, but instead looks for specific elements.
 *
 * @author vnori@google.com (Vasu Nori)
 *
 */
public class ResourceRuleParser {
  private final FileUtil fileUtil;

  @Inject
  public ResourceRuleParser(FileUtil fileUtil) {
    this.fileUtil = fileUtil;
  }

  /** parse the resource rules xml file and return list of URLs of the resources that belong to
   * THIS agent.
   *
   * @param resourcesFileName the resources xml file to be parsed
   * @param thisAgentId this agent's id
   * @return list of URLs of the resources that belong to this agent
   * @throws RegistrationException thrown if a resource rule is missing url or agentid element
   * @throws FileNotFoundException thrown if the resources xml file is not found
   * @throws XMLStreamException thrown if there is any parsing error
   * @throws FactoryConfigurationError thrown if there is any parsing error
   */
  public List<String> parseResourcesFile(String resourcesFileName, String thisAgentId)
      throws RegistrationException, FileNotFoundException,
      XMLStreamException, FactoryConfigurationError {
    FileInputStream fileInputStream = fileUtil.getFileInputStream(resourcesFileName);
    XMLStreamReader xmlStreamReader =
      XMLInputFactory.newInstance().createXMLStreamReader(fileInputStream);
    String url = null, agentId = null;
    List<String> urlList = new ArrayList<String>();
    while (xmlStreamReader.hasNext()) {
      switch (xmlStreamReader.next()) {
        case XMLStreamConstants.START_ELEMENT:
          // look for url and agentId elements.
          String currentTag = xmlStreamReader.getLocalName();
          boolean urlFound = currentTag.equalsIgnoreCase("url");
          boolean agentIdFound = currentTag.equalsIgnoreCase("agentId");
          if (urlFound || agentIdFound) {
            int event = xmlStreamReader.next();
            if (event == XMLStreamConstants.CHARACTERS) {
              // can't tolerate anything other than CHARACTERS element
              String tagValue = xmlStreamReader.getText();
              if (urlFound) {
                url = tagValue;
              } else {
                agentId = tagValue;
              }
            }
          }
          break;

        case XMLStreamConstants.END_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("rule")) {
            ensurePresenceOfAgentIdAndUrl(url, agentId);
            if (agentId.equals(thisAgentId) || agentId.equalsIgnoreCase("all")) {
              // this url is a resource served by this agent.
              urlList.add(url);
            }
            url = null;
            agentId = null;
          }
          break;
      }
    }
    xmlStreamReader.close();
    return urlList;
  }

  private void ensurePresenceOfAgentIdAndUrl(String url, String agentId)
      throws RegistrationException {
    if (url == null || agentId == null) {
      throw new RegistrationException("resources.xml file is mising url / agentId " +
            "elements in one of the resource rules");
    }
  }
}
