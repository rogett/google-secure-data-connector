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

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * First class object representing the Registration request for the Secure Link protocol.  This 
 * provides a toJson() utility to serialize information for over-the-wire communication and 
 * provides a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationRequest {

  static final String RESOURCES_KEY = "resources";
  
  // Dependencies
  private ResourceRuleUtil resourceRuleUtil;
  
  private JSONArray resourcesJsonArray;
  private List<ResourceRule> resourceRules;
  
  @Inject
  public RegistrationRequest(ResourceRuleUtil resourceRuleUtil) {
    this.resourceRuleUtil = resourceRuleUtil;
    resourceRules = new ArrayList<ResourceRule>();
  }
  
  /**
   * Creates the request from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws ResourceException if the JSON is malformed.
   */
  public void populateFromJSON(final JSONObject json) throws ResourceException {
    try {
      resourcesJsonArray = json.getJSONArray(RESOURCES_KEY);
      for (int index=0; index < resourcesJsonArray.length(); index++) {
        String resourceXml = resourcesJsonArray.getString(index);
        resourceRules.add(resourceRuleUtil.getResourceRuleFromEntityXml(resourceXml));
      }
    } catch (JSONException e) {
      throw new ResourceException(e);
    }
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   */
  public void populateFromResources(List<ResourceRule> resourceRules) throws ResourceException {
    resourcesJsonArray = new JSONArray();
    for (ResourceRule resourceRule : resourceRules) {
      resourcesJsonArray.put(resourceRuleUtil.getEntityXmlFromResourceRule(resourceRule));
      this.resourceRules.add(resourceRule);
    }
  }

  public List<ResourceRule> getResources() {
    return resourceRules;
  }
  
  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(RESOURCES_KEY, resourcesJsonArray);
    return json;
  }
}
  