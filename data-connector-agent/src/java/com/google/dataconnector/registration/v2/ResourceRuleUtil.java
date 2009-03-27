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
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;
import com.google.inject.Inject;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility methods to convert resource strings into {@link ResourceRule} list.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleUtil {

  private static final String TOP_LEVEL_ELEMENT = "resourceRules";
  private static final String ENTITY = "rule";
  private static final String ALL = "all";

  // Dependencies
  private XmlUtil xmlUtil;
  private BeanUtil beanUtil;

  /**
   * Creates a new resource util with provided dependcies
   * 
   * @param xmlUtil utility to convert XML into map properties
   * @param beanUtil utility to covert properties map into a bean.
   */
  @Inject
  public ResourceRuleUtil(XmlUtil xmlUtil, BeanUtil beanUtil) {
    this.xmlUtil = xmlUtil;
    this.beanUtil = beanUtil;
  }

  /**
   * Creates secret keys for each of the resources.
   * 
   * @param resourceRules a list of resourceRules.
   */
  public void setSecretKeys(List<ResourceRule> resourceRules) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSecretKey(new Random().nextLong());
    }
  }

  /**
   * Finds all rules that either match our client ID specified or match "all". We remove any rules
   * that are not for "us". This method modifies the list in-place.  The idea here is that 
   * ResourceRules config files could be the same for all clients.  When we move to config in the
   * cloud, there will only be one feed of resource rules.
   * 
   * @param resourceRules that correctly matches our client.
   */
  public void getThisClientRulesAndSetId(List<ResourceRule> resourceRules, 
      String myClientId) {
    List<ResourceRule> removalList = new ArrayList<ResourceRule>();
    for (ResourceRule resourceRule : resourceRules) {
      if (!resourceRule.getClientId().equals(ALL) && 
          !resourceRule.getClientId().equals(myClientId)) {
        removalList.add(resourceRule);
      } else {
        resourceRule.setClientId(myClientId);
      }
    }
    // Delete out the entries not for this client.
    resourceRules.removeAll(removalList);
  }

  /**
   * Sets the socks server listen port for these resource rules
   * 
   * @param resourceRules a list of resourceRules.
   * @param socksServerPort the first port number to use for each http rule.
   */
  public void setSocksServerPort(List<ResourceRule> resourceRules, int socksServerPort) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSocksServerPort(socksServerPort);
    }
  }

  /**
   * Return a populated {@link ResourceRule} bean from the given payload-in-content entity XML.
   * 
   * @param entityXmlText payload-in-content XML representing a ResourceRule
   * @return a populated Resource Rule bean.
   * @throws ResourceException if any XML parsing or conversion errors occur.
   */
  public ResourceRule getResourceRuleFromEntityXml(String entityXmlText) throws ResourceException {
    try {
      ResourceRule resourceRule = new ResourceRule();
      beanUtil.convertPropertiesToBean(xmlUtil.convertXmlToProperties(entityXmlText), resourceRule);
      return resourceRule;
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    } catch (SAXException e) {
      throw new ResourceException(e);
    } catch (IOException e) {
      throw new ResourceException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Returns XML generated from {@link ResourceRule} bean.
   * 
   * @param resourceRule a populated bean.
   * @return entity XML for this bean.
   * @throws ResourceException if any conversion errors occur.
   */
  public String getEntityXmlFromResourceRule(ResourceRule resourceRule) throws ResourceException {
    try {
      return xmlUtil.convertPropertiesToXml(beanUtil.convertBeanToProperties(resourceRule));
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Returns a list of {@link ResourceRule} from a feed encompassing one or more payload-in-content
   * entity XML representing a property configured Resource Rule.
   * @param xmlFeedText The feed XML with &lt;feed&gt; as the top-level element.
   * @return a list of {@link ResourceRule}.
   * @throws ResourceException if any XML parsing or conversion errors occur.
   */
  @SuppressWarnings("unchecked")
  public List<ResourceRule> getResourceRules(String xmlFeedText) throws ResourceException {
    List<ResourceRule> resourceRules = new ArrayList<ResourceRule>();
    try {
      Map<String,Object> entities = xmlUtil.convertXmlToProperties(xmlFeedText, TOP_LEVEL_ELEMENT);
      if (entities.containsKey(ENTITY)) {
        if (entities.get(ENTITY) instanceof Object[]) { 
          for (Object ruleProperties :  (Object[]) entities.get(ENTITY)) {
            if (ruleProperties instanceof HashMap) {
              ResourceRule resourceRule = new ResourceRule();  
              beanUtil.convertPropertiesToBean((HashMap) ruleProperties, resourceRule);
              resourceRules.add(resourceRule);
            }
          }
        }
      }
      return resourceRules;
    } catch (SAXException e) {
      throw new ResourceException(e);
    } catch (IOException e) {
      throw new ResourceException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceException(e);
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * For HTTPS rules use the URL util to extract the host from the URL pattern.
   * 
   * @param resourceRule the resource rule to exact host from.
   * @return a hostname.
   */
  public String getHostnameFromRule(ResourceRule resourceRule) {
    try {
      URL url = new URL(resourceRule.getPattern());
      return url.getHost();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * For HTTPS rules use the URL util to extract the port from the URL pattern.
   * 
   * @param resourceRule the resource rule to exact host from.
   * @return a port.
   */
  public Integer getPortFromRule(ResourceRule resourceRule) {
    try {
      URL url = new URL(resourceRule.getPattern());
      int port = url.getPort();
      if (port != -1) {
        return port;
      }
      // not specified
      return (resourceRule.getPattern().startsWith(ResourceRule.HTTPSID)) ? 443 : 80;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * creates the following system resource rules
   * 
   * 1. healthcheck rule:  http://localhost:portnum/<clientId>/__SDCINTERNAL__/healthcheck
   * 2. rules to allow access to healthcheck feeds (TODO - after enabling routing options to be
   * declared for each resource rule)
   * 
   * @param user the userid who should be allowed to access this resource
   * @param domain the domain the above user belongs to
   * @param clientId the clientId this resource is attached to
   * @param port the port HealthCheckRequestHandler is listening on
   * @param healthCheckGadgetUsers the users who are allowed access to the healthcheck gadget.
   * @return the list of system resources created
   */
  public List<ResourceRule> createSystemRules(String user, String domain, String clientId, 
      int port, String healthCheckGadgetUsers) {
    List<ResourceRule> systemRules = new ArrayList<ResourceRule>();
    int nextRuleNum = Integer.MAX_VALUE;
    
    // if the input param healthCheckGadgetUsers is not null, add the input user to the list of 
    // users
    String[] allowedEntities;
    String implicitUser = user + "@" + domain;
    if (healthCheckGadgetUsers != null) {
      allowedEntities = (healthCheckGadgetUsers + "," + implicitUser).split(",");
    } else {
      allowedEntities = new String[] {implicitUser};
    }
    
    // create healthcheck rule
    ResourceRule healthCheckRule = new ResourceRule();
    healthCheckRule.setAllowedEntities(allowedEntities);
    healthCheckRule.setClientId(clientId);
    AppTag app = new AppTag();
    // TODO(josecasillas): Add a more restrictive rule for implicit rules.
    app.setContainer(".*");
    app.setAppId(".*");
    AppTag[] array = new AppTag[1];
    array[0] = app;
    healthCheckRule.setApps(array);
    // assign name of Integer.MAX_VALUE
    healthCheckRule.setRuleNum(nextRuleNum--);
    healthCheckRule.setPattern(ResourceRule.HTTPID + "localhost:" + port + "/" + clientId + 
	  "/__SDCINTERNAL__/healthcheck");
    healthCheckRule.setPatternType(ResourceRule.URLEXACT);
    systemRules.add(healthCheckRule);
    return systemRules;
  }
  
  /**
   * For legacy clients, sets rule number from deprecated name. 
   * TODO(rayc) remove when we deprecate TT2 and older clients.
   */
  @Deprecated
  public void setRuleNumFromName(List<ResourceRule> resourceRules) throws ResourceException {
    for (ResourceRule resourceRule : resourceRules) {
      if (resourceRule.getName() != null) {
        try {
          resourceRule.setRuleNum(Integer.valueOf(resourceRule.getName()));
        } catch (NumberFormatException e) {
          throw new ResourceException(e);
        }
      }
    }
  }
}
