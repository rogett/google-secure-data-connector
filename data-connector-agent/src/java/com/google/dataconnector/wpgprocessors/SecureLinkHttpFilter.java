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
package com.google.dataconnector.wpgprocessors;

import com.wpg.proxy.HttpMessageRequest;
import com.wpg.proxy.HttpMessageRequestProcessor;

import java.util.List;

/**
 * Implements a {@link HttpMessageRequestProcessor} that compares the incoming URL to our
 * allowed regexp list.  
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SecureLinkHttpFilter implements HttpMessageRequestProcessor {

  private List<String> ruleSets;
  
  /**
   * Creates the filter from the list of rulesets.
   * 
   * @param ruleSets a list of regexp patterns that will match allowed URLs.
   */
  public SecureLinkHttpFilter(List<String> ruleSets) {
    this.ruleSets = ruleSets;    
  }

  /**
   * We should always process more filters after this one
   */
  public boolean doContinue(HttpMessageRequest input) {
    return true;
  }

  /**
   * Look at the message and see if it matches a ruleset
   * configured for this filter.
   * 
   * @return true if allowed false if not.
   */
  public boolean doSend(HttpMessageRequest message) {

    for (String ruleSet : ruleSets) {      
      if (message.getUri().toString().matches(ruleSet)) {
        return true; 
      }
    }
    // no matches
    return false;
  }

  /**
   * This filter does no processing currently we just
   * return the message as-passed.
   */
  public HttpMessageRequest process(HttpMessageRequest message) {
    return message;
  }

}
