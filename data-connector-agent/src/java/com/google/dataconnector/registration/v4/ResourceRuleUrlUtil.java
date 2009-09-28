/* Copyright 2009 Google Inc.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods to handle Url in ResourceRules
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class ResourceRuleUrlUtil {

  public enum Scheme {
    HTTP,
    HTTPS,
    SOCKET;

    public static List<String> getValidValues() {
      Scheme[] schemes = Scheme.values();
      int len = schemes.length;
      List<String> validValues = new ArrayList<String>();
      for (int i = 0; i < len; i++) {
        validValues.add(schemes[i].name().toLowerCase());
      }
      return validValues;
    }
  }

  /**
   * return the scheme from given given URL. an exception is thrown
   * if it is not http or https or socket
   *
   * @param resourceRuleUrl the string representing the url
   * @return the Scheme enum object
   * @throws ResourceUrlException thrown if the url is not URI format or if the scheme is
   * not http or https or socket
   */
  public Scheme getSchemeInUrl(String resourceRuleUrl) throws ResourceUrlException {
    try {
      URI uri = new URI(resourceRuleUrl);
      String scheme = uri.getScheme();
      return Scheme.valueOf(scheme.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResourceUrlException("resource url can only start with " +
          Scheme.getValidValues());
    } catch (URISyntaxException e) {
      throw new ResourceUrlException("badly formed resource url " + resourceRuleUrl);
    }
  }

  /**
   * extract the hostname from the given url.
   * by using URI, the given URL will not be resolved.
   */
  public String getHostnameFromRule(String resourceRuleUrl) throws ResourceUrlException {
      // validate the uri
      getSchemeInUrl(resourceRuleUrl);
      try {
        URI uri = new URI(resourceRuleUrl);
        return uri.getHost();
      } catch (URISyntaxException e) {
        throw new ResourceUrlException("badly formed resource url " + resourceRuleUrl);
      }
  }

  /**
   * Use URI util to extract the port from the given url.
   * by using URI, the given URL will not be resolved.
   */
  public int getPortFromRule(String resourceRuleUrl) throws ResourceUrlException {
      // validate the uri
      Scheme scheme = getSchemeInUrl(resourceRuleUrl);
      URI uri;
      try {
        uri = new URI(resourceRuleUrl);
      } catch (URISyntaxException e) {
        throw new ResourceUrlException("badly formed resource url " + resourceRuleUrl);
      }
      int port = uri.getPort();
      if (port != -1) {
        return port;
      } else {
        // port not specified
        return (scheme == Scheme.HTTPS) ? 443 : 80;
      }
  }
}
