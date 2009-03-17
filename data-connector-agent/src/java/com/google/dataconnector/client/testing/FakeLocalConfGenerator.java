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
package com.google.dataconnector.client.testing;

import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.dataconnector.util.LocalConf;
import com.google.feedserver.client.FeedServerEntry;
import com.google.feedserver.util.ContentUtil;
import com.google.gdata.data.OtherContent;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeLocalConfGenerator {

  /** The fake properties file we generate the config from */
  private LocalConf fakeLocalConf;
  
  /* conf values */
  public static final String NAME = "localConf";
  public static final String SDC_SERVER_HOST = "test.apps-secure-data-connector.google.com";
  public static final Integer SDC_SERVER_PORT = 443;
  public static final String DOMAIN = "test.joonix.net";
  public static final String USER = "testuser";
  public static final String PASSWORD = "testpassword";
  public static final String SSL_KEY_STORE_PASSWORD = "woodstock";
  public static final String SSL_KEY_STORE_FILE = "./testSecureLinkClientTrustStore";
  public static final String CLIENT_ID = "testClientId1";
  public static final String SSHD = "./sshd";
  public static final String HTTP_PROXY_BIND_HOST = "127.0.0.1";
  public static final Integer SOCKS_SERVER_PORT = 1080;
  public static final String SOCKSD_BIND_HOST = "127.0.0.1";
  public static final String RULES_FILE = "/tmp/rulesConf.xml";
  
  /**
   * Creates a configuration beans from the fake hardcoded XML files.  
   */
  public FakeLocalConfGenerator() {

    FeedServerEntry configEntry = new FeedServerEntry(CONFIG_XML);
    fakeLocalConf = new LocalConf();
    // TODO: write tests for LocalConf.AuthType.PASSWORD also
    fakeLocalConf.setAuthType(AuthRequest.AuthType.PASSWORD);
    ContentUtil contentUtil = new ContentUtil();
    try {
      contentUtil.fillBean((OtherContent) configEntry.getContent(), fakeLocalConf);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return localConf populated with a fake set of configuration from {@link #CONFIG_XML}
   */
  public LocalConf getFakeLocalConf() {
    return fakeLocalConf;
  }
  
  public static final String CONFIG_XML =  "<entity>\n" +
    "<name>" + NAME +"</name>\n" +
    "<rulesFile>" + RULES_FILE + "</rulesFile>\n" +
    "<sdcServerHost>" + SDC_SERVER_HOST + "</sdcServerHost>\n" +
    "<sdcServerPort>" + SDC_SERVER_PORT + "</sdcServerPort>\n" +
    "<domain>" + DOMAIN + "</domain>\n" +
    "<user>" + USER + "</user>\n" +
    "<password>" + PASSWORD + "</password>\n" +
    "<sslKeyStorePassword>" + SSL_KEY_STORE_PASSWORD + "</sslKeyStorePassword>\n" +
    "<sslKeyStoreFile>" + SSL_KEY_STORE_FILE +"</sslKeyStoreFile>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<sshd>\n" + SSHD + "</sshd>\n" +
    "<httpProxyBindHost>" + HTTP_PROXY_BIND_HOST + "</httpProxyBindHost>\n" +
    "<socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "<socksdBindHost>" + SOCKSD_BIND_HOST + "</socksdBindHost>\n" +
    // We hard code socks properties and log properties because they are not used in our tests.
    "<socksProperties>\n" +
    "iddleTimeout    = 600000   # 10 minutes\n" +
    "acceptTimeout   = 60000    # 1 minute\n" +
    "udpTimeout      = 600000   # 10 minutes\n" +
    "log = -\n" +
    "</socksProperties>\n" +
    "</entity>\n";
}
