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
package com.google.dataconnector.client.testing;

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
  public static final String OAUTH_KEY = "testOauthKey";
  public static final Boolean USE_SSL = true;
  public static final String SSL_KEY_STORE_PASSWORD = "woodstock";
  public static final String SSL_KEY_STORE_FILE = "./testSecureLinkClientTrustStore";
  public static final String CLIENT_ID = "testClientId1";
  public static final String SSHD = "./sshd";
  public static final Integer HTTP_PROXY_PORT = 31823;
  public static final String HTTP_PROXY_BIND_HOST = "127.0.0.1";
  public static final Integer SOCKS_SERVER_PORT = 1080;
  public static final String SOCKSD_BIND_HOST = "127.0.0.1";
  public static final String RULES_FILE = "/tmp/rulesConf.xml";
  public static final String APACHE_CONF_DIR = "/tmp/apache";
  public static final String APACHE_ROOT = "/tmp/apacheroot";
  
  /**
   * Creates a configuration beans from the fake hardcoded XML files.  
   */
  public FakeLocalConfGenerator() {

    FeedServerEntry configEntry = new FeedServerEntry(CONFIG_XML);
    fakeLocalConf = new LocalConf();
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
    "<oauthKey>" + OAUTH_KEY + "</oauthKey>\n" +
    "<useSsl>" + USE_SSL.toString() + "</useSsl>\n" +
    "<sslKeyStorePassword>" + SSL_KEY_STORE_PASSWORD + "</sslKeyStorePassword>\n" +
    "<sslKeyStoreFile>" + SSL_KEY_STORE_FILE +"</sslKeyStoreFile>\n" +
    "<clientId>" + CLIENT_ID + "</clientId>\n" +
    "<sshd>\n" + SSHD + "</sshd>\n" +
    "<httpProxyPort>" + HTTP_PROXY_PORT + "</httpProxyPort>\n" +
    "<httpProxyBindHost>" + HTTP_PROXY_BIND_HOST + "</httpProxyBindHost>\n" +
    "<socksServerPort>" + SOCKS_SERVER_PORT + "</socksServerPort>\n" +
    "<socksdBindHost>" + SOCKSD_BIND_HOST + "</socksdBindHost>\n" +
    "<apacheConfDir>" + APACHE_CONF_DIR + "</apacheConfDir>\n" +
    "<apacheRoot>" + APACHE_ROOT + "</apacheRoot>" +
    // We hard code socks properties and log properties because they are not used in our tests.
    "<socksProperties>\n" +
    "iddleTimeout    = 600000   # 10 minutes\n" +
    "acceptTimeout   = 60000    # 1 minute\n" +
    "udpTimeout      = 600000   # 10 minutes\n" +
    "log = -\n" +
    "</socksProperties>\n" +
    "<logProperties>\n" +
    "# ***** Set root logger level to DEBUG and its only appender to A.\n" +
    "log4j.rootLogger=debug, A\n" +
    "\n" +
    "# ***** A is set to be a ConsoleAppender.\n" +
    "log4j.appender.A=org.apache.log4j.ConsoleAppender\n" +
    "# ***** A uses PatternLayout.\n" +
    "log4j.appender.A.layout=org.apache.log4j.PatternLayout\n" +
    "log4j.appender.A.layout.ConversionPattern=%-4r [%t] %-5p %c %x - %m%n\n" +
    "</logProperties>\n" +
    "</entity>\n";
}
