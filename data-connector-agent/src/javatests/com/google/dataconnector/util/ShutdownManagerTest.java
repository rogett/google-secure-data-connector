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
package com.google.dataconnector.util;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for the {@link ShutdownManager} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ShutdownManagerTest extends TestCase {
  
  private static final String GROUP1 = "group1";
  private static final String GROUP2 = "group2";
  
  public void testAddStoppableDefaultAndShutdownAll() {
    
    // Setup
    List<MockStoppable> stoppables = Lists.newArrayList();
    ShutdownManager shutdownManager = new ShutdownManager();
    
    for (int count = 1; count <= 5; count++) {
      MockStoppable stoppable = new MockStoppable();
      stoppables.add(stoppable);
      shutdownManager.addStoppable(stoppable);
    }
    
    // Execute
    shutdownManager.shutdownAll(); // should call shutdown on all stoppables once.
    shutdownManager.shutdownAll(); // these should be purged so nothing should happen.
    
    // Verify
    for (MockStoppable stoppable : stoppables) {
      assertEquals(1, stoppable.getTotalShutdownCalls());   
    }
  }
  
  public void testGroupAddStoppableAndShutdownGroup() {
    // Setup
    MockStoppable stoppable1 = new MockStoppable();
    MockStoppable stoppable2 = new MockStoppable();
    
    ShutdownManager shutdownManager = new ShutdownManager();
    shutdownManager.addStoppable(stoppable1, GROUP1);
    shutdownManager.addStoppable(stoppable2, GROUP2);
    
    // Execute
    shutdownManager.shutdownGroup(GROUP1);
    shutdownManager.shutdownGroup(GROUP1); // group should be purged so this 
    
    // Verify
    assertEquals(0, stoppable2.getTotalShutdownCalls()); // group 2 should be untouched.
    assertEquals(1, stoppable1.getTotalShutdownCalls());
  }
  
  /**
   * Mock stoppable implementation that records whether shutdown was actually called.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public static class MockStoppable implements Stoppable {

    int totalShutdownCalls;
    
    @Override
    public void shutdown() {
      totalShutdownCalls++;
    }
    
    public int getTotalShutdownCalls() {
      return totalShutdownCalls;
    }
  }

}
