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

import com.google.dataconnector.util.ClientConf;
import com.google.dataconnector.util.ConfigurationException;
import com.google.dataconnector.util.ResourceConfigEntry;
import com.google.dataconnector.util.ResourceConfigException;
import com.google.dataconnector.util.SocketResourceConfigEntry;
import com.google.dataconnector.util.UriResourceConfigEntry;

import org.apache.commons.cli.CommandLine;
import org.easymock.classextension.EasyMock;
import org.json.JSONArray;

import java.util.Properties;

/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeClientConfiguration {

  /** We do not generate random keys but instead start at 1 and increment 1 for each rule */
  public static final Long STARTING_VALID_KEY = 1L;

  /** The fake properties file we generate the config from */
  private Properties fakeClientProps;

  /** The fake client configuration object */
  private ClientConf fakeClientConf;

  /**
   * Creates a {@link ClientConf} object from the fake hardcoded properties.  This class mocks
   * the flags calls since we do not want to require a commandline to parse.
   *
   * @throws ConfigurationException if the fake hardcoded properties has an error.
   */
  public FakeClientConfiguration() throws ConfigurationException {

    fakeClientProps = getFakeClientProperties();
    ClientConf.setFlagsForTest(EasyMock.createMock(CommandLine.class));
    CommandLine flags = ClientConf.getFlagsForTest();
    Properties clientProps = getResourceProperties();
    for (String keyName : clientProps.stringPropertyNames()) {
      EasyMock.expect(flags.getOptionValue(keyName, null)).andReturn(null).anyTimes();
      EasyMock.expect(flags.getOptionValue(keyName, clientProps.getProperty(
          keyName))).andReturn(clientProps.getProperty(keyName)).anyTimes();

    }
    EasyMock.expect(flags.hasOption("nouseSsl")).andReturn(true);
    EasyMock.replay(flags);
    fakeClientConf = new ClientConf(getResourceProperties());
  }

  /**
   * @return ClientConf populated with a fake set of configuration from
   *             {@link #getResourceProperties}
   */
  public ClientConf getFakeClientConf() {
    return fakeClientConf;
  }

  /**
   * @return Properties populated with faked but valid resource configuration data.
   */
  public Properties getResourceProperties() {
    return fakeClientProps;
  }

  /**
   * Creates a {@link JSONArray} using the faked data from {@link #getResourceProperties}
   *
   * @return JSONArray populated with faked data.
   * @throws ResourceConfigException this should never happen.
   */
  public JSONArray getResourceJsonArray() throws ResourceConfigException {
    JSONArray testArray = new JSONArray();
    Long secretKey = STARTING_VALID_KEY;
    for (String keyName : fakeClientProps.stringPropertyNames()) {
      ResourceConfigEntry entry;
      String[] resourceStr =
          ClientConf.parseResourceRule(fakeClientProps.getProperty(keyName));
      if (keyName.startsWith("socketrule")) {
        entry = new SocketResourceConfigEntry(secretKey,
            resourceStr[0], resourceStr[1], 1, resourceStr[2]);
      } else if (keyName.startsWith("httprule")) {
        entry = new UriResourceConfigEntry(secretKey,
            resourceStr[0], resourceStr[1], 3128, 2, resourceStr[2]);
      } else {
        continue;
      }
      testArray.put(entry.toJSON());
      secretKey++;
    }
    return testArray;
  }

  /**
   * Creates the client properties file from the hardcoded values below.
   *
   * @return Properties instance with the hardcoded secure link client configuration.
   */
  public static Properties getFakeClientProperties() {
    Properties clientProps = new Properties();
    clientProps.setProperty("clientId", "friendly");
    clientProps.setProperty("domain", "joonix.net");
    clientProps.setProperty("user", "rcolline");
    clientProps.setProperty("password", "foobad");
    clientProps.setProperty("socksServerPort", "1080");
    clientProps.setProperty("secureLinkServerHost", "localhost");
    clientProps.setProperty("secureLinkServerPort", "4444");
    clientProps.setProperty("startingHttpProxyPort", "3128");
    clientProps.setProperty("resource1", "socket://128.195.131.4:143 user1,user2,group1");
    clientProps.setProperty("resource2", "http://.*.corp.example.com:.*/.* user1,user2,group1");
    clientProps.setProperty("resource3", 
        "http://.*.corp.example.com:.*/.* user3,user4 srcid1,srcid2");
    clientProps.setProperty("sshd", "third_party/foo/bar/sshd");
    clientProps.setProperty("logPropertiesFile", "logging.properties");
    clientProps.setProperty("sslKeyStorePassword", "sslKeyStorePassword");
    clientProps.setProperty("sslKeyStoreFile", "sslKeyStoreFile");
    clientProps.setProperty("httpProxyBindHost", "httpProxyBindHost");
    clientProps.setProperty("socksdBindHost", "socksdBindHost");
    return clientProps;
  }
}
