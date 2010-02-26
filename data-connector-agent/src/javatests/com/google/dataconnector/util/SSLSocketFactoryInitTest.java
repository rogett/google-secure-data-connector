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

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import javax.net.ssl.SSLSocketFactory;

public class SSLSocketFactoryInitTest extends TestCase {

  private LocalConf localConf;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    localConf = EasyMock.createMock(LocalConf.class);
  }

  @Override public void tearDown() throws Exception {
    localConf = null;
    super.tearDown();
  }

  public void testGetSslSocketFactoryUseDefaultKeystore() {
    EasyMock.expect(localConf.getSslKeyStoreFile()).andReturn(null);
    EasyMock.expect(localConf.getAllowUnverifiedCertificates()).andReturn(false);
    EasyMock.replay(localConf);

    SSLSocketFactoryInit sSLSocketFactoryInit = new SSLSocketFactoryInit(null);
    SSLSocketFactory factory = sSLSocketFactoryInit.getSslSocketFactory(localConf);
    assertNotNull(factory);

    EasyMock.verify(localConf);
  }

  public void testGetSslSocketFactoryUseGivenKeystore() {
    EasyMock.expect(localConf.getSslKeyStoreFile()).andReturn("test_keystorefile");
    EasyMock.expect(localConf.getSslKeyStorePassword()).andReturn("test_password");
    EasyMock.expect(localConf.getAllowUnverifiedCertificates()).andReturn(true);
    EasyMock.replay(localConf);

    SSLSocketFactoryInit sSLSocketFactoryInit = new SSLSocketFactoryInit(null);
    SSLSocketFactory factory = sSLSocketFactoryInit.getSslSocketFactory(localConf);
    assertNotNull(factory);

    EasyMock.verify(localConf);
  }
}
