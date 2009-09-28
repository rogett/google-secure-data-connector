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
 *
 * $Id$
 */
package com.google.dataconnector.util;

import com.google.dataconnector.protocol.ProtocolGuiceModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

/**
 * Client Guice module.
 *
 * TODO(rayc) write unit tests for this module.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class ClientGuiceModule extends AbstractModule {

  private static final int MAX_THREADS = 500;
  private static Injector injector = null;

  @Override
  protected void configure() {}

  public static Injector getInjector() {
    if (injector == null) {
      injector = Guice.createInjector(new ClientGuiceModule(), new ProtocolGuiceModule());
    }
    return injector;
  }

  @Provides @Singleton
  public SocketFactory getSocketFactory() {
    return SocketFactory.getDefault();
  }

  @Provides @Singleton
  public ThreadPoolExecutor getThreadPoolExecutor() {
    return new ThreadPoolExecutor(50, MAX_THREADS, 60L, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>());
  }

  /**
   * Provides the runtime for methods needing to make processes.
   *
   * @return the VM's runtime.
   */
  @Provides
  public Runtime getRuntime() {
    return Runtime.getRuntime();
  }

  @Provides @Singleton @Named("Socks Properties")
  public Properties getSocksProperties(LocalConf localConf) {
    Properties properties = new Properties();
    try {
      properties.load(new ByteArrayInputStream(localConf.getSocksProperties().trim().getBytes()));
      return properties;
    } catch (IOException e) {
      throw new RuntimeException("Invalid socks properties", e);
    }
  }

  /**
   * creates a singleton instance of {@link HealthCheckRequestHandler} with a
   * ServerSocket listening on an ephemeral port.
   *
   * @return created singleton instance of HealthCheckRequestHandler
   * @throws IOException thrown if ServerSocket couldn't be created
   */
  @Provides @Singleton
  public HealthCheckRequestHandler getHealthCheckRequestHandler() throws IOException {
    return new HealthCheckRequestHandler(new ServerSocket(0));
  }

  @Provides @Singleton @Named("localhost")
  public InetAddress getLocalHostInetAddress() {
    try {
      return Inet4Address.getByName("127.0.0.1");
    } catch (UnknownHostException e) {
      throw new RuntimeException("Could not resolve localhost", e);
    }
  }

}
