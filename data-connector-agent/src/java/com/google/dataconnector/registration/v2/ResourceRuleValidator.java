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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a resource rule and produces errors.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleValidator {
  
  private static final int MAX_PORT = 65535;
  
  /**
   * Iterates through the provided list of resource rules and validates each one against the
   * required runtime configuration.
   * 
   * @param resourceRules a list of resource rules
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validateRuntime(List<ResourceRule> resourceRules) 
      throws ResourceException {
    for (ResourceRule resourceRule : resourceRules) {
      validateRuntime(resourceRule);
    }
  }
  
  /**
   * Iterates through the provided list of resource rules and validates each one against the
   * required configuration.
   * 
   * @param resourceRules a list of resource rules
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validate(List<ResourceRule> resourceRules) 
      throws ResourceException {
    
    // Bail if we have 0 rules.  The only known way to get here is if you leave off "repeatable"
    // attribute from the <entity> element in the resourceRules.xml file.  Normally if you 
    // specify no rules in that file, a different worse error happens.
    // See bug: 1538670
    if (resourceRules.size() == 0 ) {
      throw new ResourceException("Must specify atleast one rule.  This may be caused by an XML" +
          "parsing issue where you must specify '<entity repeatable=\"true\">' even if one" +
          "rule is specified."); 
    }
     
    // Go through each rule and validate it and make sure no two resources have the same seqNum.
    Set<Integer> seenSeqNums = new HashSet<Integer>();
    for (ResourceRule resourceRule : resourceRules) {
      validate(resourceRule);
      if (seenSeqNums.contains(resourceRule.getSeqNum())) {
        throw new ResourceException("Duplicate <seqNum/> entries not allowed. Resource: " + 
            resourceRule.getSeqNum());
      } else {
        seenSeqNums.add(resourceRule.getSeqNum());
      }
    }
  }
  
  /**
   * Validates a single resource rule against the required runtime configuration.  
   * 
   * @param resourceRule a resource rule configured for runtime usage.
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validateRuntime(ResourceRule resourceRule) 
      throws ResourceException {
    
    // All Config time validation and ...
    validate(resourceRule);
    
    // httpProxyPort - required for all HTTP resources
    // TODO(rayc) remove when we eliminate wpg-proxy
    Integer httpProxyPort = resourceRule.getHttpProxyPort();
    if (httpProxyPort != null) {
      if (httpProxyPort > MAX_PORT || httpProxyPort < 0) {
        throw new ResourceException("HttpProxyPort " + httpProxyPort + " out of range.");
      }
    } else if (resourceRule.getPattern().trim().startsWith(ResourceRule.HTTPID)) {
      throw new ResourceException("'httpProxyPort' required for each " + ResourceRule.HTTPID +
          "resource");
    }
    
    // socksServerPort 
    Integer socksServerPort = resourceRule.getSocksServerPort();
    if (socksServerPort != null) {
      if (socksServerPort > MAX_PORT || socksServerPort < 0) {
        throw new ResourceException("socksServerPort " + socksServerPort + " out of range.");
      }
    } else {
      throw new ResourceException("'socksServerPort' required for each resource");
    }
    
    // secretKey
    if (resourceRule.getSecretKey() == null) {
      throw new ResourceException("Rule is missing secret key");
    }
  }
  
  /**
   * Validates a single resource rule against the required configuration.  
   * 
   * @param resourceRule a resource rule at configuration time.
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validate(ResourceRule resourceRule) throws ResourceException {
    
    // Name - this is not to be used anymore.
    if (resourceRule.getName() != null) {
      throw new ResourceException("Resource " +  resourceRule.getName() + " uses deprecated " +
          "<name/> element.  Please use <seqNum>" + resourceRule.getName() + "</seqNum> instead.");
    }
    
    // seqNum
    if (resourceRule.getSeqNum() > 0) {
      // set name to seqNum
      resourceRule.setName(String.valueOf(resourceRule.getSeqNum()));
    } else {
      throw new ResourceException("Resource " + resourceRule.getPattern() + "must have <seqNum/> " +
          "greater than 0.");
    }
    
    // clientId
    if (resourceRule.getClientId() != null) {
       if (resourceRule.getClientId().trim().contains(" ")) {
         throw new ResourceException("'clientId' field " + resourceRule.getClientId() + 
             " must not contain any white space.");
       }
    } else { 
      throw new ResourceException("'clientId' field must be present");
    }
    
    // allowed entities
    if (resourceRule.getAllowedEntities() != null) {
      for (String allowedEntity : resourceRule.getAllowedEntities()) {
        if (allowedEntity.trim().contains(" ")) {
           throw new ResourceException(
               "'allowedEntities' field " + allowedEntity + " must not contain any white space.");
        }
        if (!allowedEntity.trim().contains("@")) {
           throw new ResourceException(
               "'allowedEntities' field " + allowedEntity + " must be a valid fully qualified " +
                     "email address");
          
        }
      }
    } else {
      throw new ResourceException("at least one 'allowedEntities' field must be present");
    }
    
    // appids - not required
    if (resourceRule.getAppIds() != null) {
      for (String appId : resourceRule.getAppIds()) {
        if (appId.trim().contains(" ")) {
           throw new ResourceException(
               "'appIds' field " + appId + " must not contain any white space.");
        }
      }
    } 
    
    // pattern
    String pattern = resourceRule.getPattern();
    if (pattern != null) {
      pattern = pattern.trim();
      if (pattern.contains(" ")) {
        throw new ResourceException("'pattern' field " + pattern + 
            " must not contain any white space.");
      }
      if (!pattern.startsWith(ResourceRule.HTTPID) && 
          !pattern.startsWith(ResourceRule.HTTPSID) && 
          !pattern.startsWith(ResourceRule.SOCKETID)) {
        throw new ResourceException("Invalid pattern, missing identifier: " + pattern);
      }
    } else {
      throw new ResourceException("'pattern' must be present.");
    }
  }
}
