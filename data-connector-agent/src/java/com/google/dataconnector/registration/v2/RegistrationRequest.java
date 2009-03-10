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
  
