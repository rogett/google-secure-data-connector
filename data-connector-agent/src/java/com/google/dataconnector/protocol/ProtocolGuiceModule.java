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
 */ 
package com.google.dataconnector.protocol;

import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Guice module for the protocol library.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ProtocolGuiceModule extends AbstractModule {
  
  private static final int SEND_QUEUE_SIZE = 10 * 1024; // 10k entries

  @Override
  protected void configure() {}
  
  @Provides
  public BlockingQueue<FrameInfo> getFrameInfoBlockingQueue() {
    return new LinkedBlockingQueue<FrameInfo>(SEND_QUEUE_SIZE);
  }
  
  @Provides
  public BlockingQueue<SocketDataInfo> getSocketDataInfoBlockingQueue() {
    return new LinkedBlockingQueue<SocketDataInfo>(SEND_QUEUE_SIZE);
  }

}
