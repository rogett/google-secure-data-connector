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

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * Client Guice module.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationV2GuiceModule extends AbstractModule {
  
  @Override
  protected void configure() {
    bind(SocketAddress.class).annotatedWith(RegistrationV2Annotations.MyHostname.class)
        .toProvider(LocalHostEphemeralSocketAddress.class).in(Scopes.SINGLETON);
  }
  
  /**
   * Provides the system default socket factory.
   * @return socket factory.
   */
  @Provides 
  public SocketFactory getSocketFactory() {
    return SocketFactory.getDefault();
  }
  
  /**
   * Provides an SocketAddress configured for localhost with ephemeral port.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public static class LocalHostEphemeralSocketAddress implements Provider<SocketAddress> {
    
    public SocketAddress get() {
	  try {
	    return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
	  } catch (UnknownHostException e) {
	    throw new RuntimeException(e);
	  }
    }
    
  }
} 
