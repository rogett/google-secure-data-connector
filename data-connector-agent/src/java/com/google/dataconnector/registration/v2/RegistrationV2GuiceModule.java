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