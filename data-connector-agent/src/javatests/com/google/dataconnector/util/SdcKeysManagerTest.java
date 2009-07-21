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
package com.google.dataconnector.util;


import com.google.common.collect.Multimap;
import com.google.dataconnector.protocol.proto.SdcFrame.ResourceKey;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link LocalConfValidator} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class SdcKeysManagerTest extends TestCase {

  public void testProperConfigResourceRules() {
    
    List<ResourceKey> resourceKeysList = new ArrayList<ResourceKey>();
    resourceKeysList.add(buildKeyObj("ip1", 1, 111));
    resourceKeysList.add(buildKeyObj("ip2", 2, 122));
    resourceKeysList.add(buildKeyObj("ip3", 3, 133));

    // store the keys
    SdcKeysManager sdcKeysManager = new SdcKeysManager();
    sdcKeysManager.storeSecretKeys(resourceKeysList);
    
    // verify that the keys are stored correctly
    Multimap<String, Pair<String, Integer>> keysMap = sdcKeysManager.getKeysMap();
    assertTrue(keysMap.containsEntry("111", Pair.of("ip1", 1)));
    assertTrue(keysMap.containsEntry("122", Pair.of("ip2", 2)));
    assertTrue(keysMap.containsEntry("133", Pair.of("ip3", 3)));
    assertEquals(3, keysMap.size());
    
    // make sure the methods to verify correctly
    assertTrue(sdcKeysManager.checkKeyIpPort("111", "ip1", 1));
    assertTrue(sdcKeysManager.checkKeyIpPort("122", "ip2", 2));
    assertTrue(sdcKeysManager.checkKeyIpPort("133", "ip3", 3));
    assertTrue(sdcKeysManager.containsKey("111"));
    assertTrue(sdcKeysManager.containsKey("122"));
    assertTrue(sdcKeysManager.containsKey("133"));
    
    // some negative testing
    assertFalse(sdcKeysManager.checkKeyIpPort("444", "ip1", 1));
    assertFalse(sdcKeysManager.checkKeyIpPort("122", "ip2", 22));
    assertFalse(sdcKeysManager.checkKeyIpPort("133", "ip33", 3));
    assertFalse(sdcKeysManager.containsKey("444"));
    assertFalse(sdcKeysManager.containsKey("0"));
    assertFalse(sdcKeysManager.containsKey(" "));
  }
  
  private ResourceKey buildKeyObj(String ip, int port, long key) {
    return ResourceKey.newBuilder().setKey(key).setIp(ip).setPort(port).build();
  }
}
