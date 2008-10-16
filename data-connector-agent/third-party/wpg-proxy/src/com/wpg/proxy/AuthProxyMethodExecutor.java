/*
 * Java HTTP Proxy Library (wpg-proxy), more info at
 * http://wpg-proxy.sourceforge.net/
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * 
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package com.wpg.proxy;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the interface HttpMessageExecutor
 * {@link HttpMessageExecutor} Its main purpose is to provide an authentication
 * mechanism for the proxy server. If the requested domain requires
 * authentication then client's credentials will be checked . If credentials
 * have not been sent or don't match credentials supplied in properties then it
 * returns http 407 response to the client without contacting the target host.
 * Otherwise it returns null.
 * 
 */
public class AuthProxyMethodExecutor implements HttpMessageExecutor {

  /** The logger object used for all logging activities. */
  private static Logger logger =
      Logger.getLogger(AuthProxyMethodExecutor.class);

  /**
   * This is a default user of the proxy - used whenever username is not
   * specified in the rule.
   */
  public static final String DEFAULT_USER = "proxyuser";

  /**
   * This is a separator between optional username and password in a
   * configuration file.
   */
  public static final char PROPERTIES_USER_PASSWORD_SEPARATOR = '.';

  /**
   * This is a separator between username and password as used in HTTP header.
   */
  public static final char CREDENTIALS_USER_PASSWORD_SEPARATOR = ':';

  /**
   * Authentication method - represents basic methods. Other methods (Digest
   * Authentication, NTLM, etc) are not implemented at the moment
   */
  public static final String BASIC_AUTHENTICATION_METHOD = "Basic";

  /** The separator token between patterns in the rule. */
  public static final String URL_PATTERNS_SEPARATOR = " ";

  /** This token is used to indicate that url doesn't need an authentication. */
  public static final String UNPROTECTED_URL_TOKEN = "*";

  /** HTTP Response code used to request user credentials from the client. */
  public static final String HTTP_407_RESPONSE =
      "HTTP/1.0 407 Proxy Authentication Required";

  /**
   * The map contains username:password as keys and values are arrays of Regex
   * patterns for urls that are available for these credentials. This map is
   * initialized by parseProperties private method called from the constructor.
   * The configuration property object is passed as an argument to the
   * constructor.
   */
  private Map<String, List<Pattern>> urlPatternsMap =
      new HashMap<String, List<Pattern>>();

  /**
   * Constructs an instance of the class for a given property object.
   * 
   * @param prop - property object containing all authentication rules
   */
  public AuthProxyMethodExecutor(final Properties prop) {
    parseProperties(prop);
  }

  /**
   * Constructs an instance of the class for a given authentication rule map.
   * 
   * @param map - maps [username:]password (username can be omitted, the default
   *        username will be used in this case) to a list of url patterns that
   *        are authorized by the key. All unprotected urls can be mapped using
   *        special UNPROTECTED_URL_TOKEN as a replacement for the password.
   */
  public AuthProxyMethodExecutor(final Map<String, List<Pattern>> map) {
    urlPatternsMap = map;
  }

  /**
   * The interface method. Returns 407 response if authentication is required,
   * otherwise null
   * 
   * @param request - input Request object to execute
   * @return - Response object to be sent to the client
   */
  public final HttpMessageResponse executeRequest(
      final HttpMessageRequest request) {
    String host = request.getToHost();
    if (checkUnprotectedUrl(host)) {
      return null;
    }
    String credentials = getCredentials(request);
    if (checkUrl(credentials, host)) {
      return null;
    }
    return createAuthRequiredResponse(host);
  }

  /**
   * Creates HTTP 407 response object.
   * 
   * @param host - Host string of the client
   * @return = Response object to be sent to client
   */
  private HttpMessageResponse createAuthRequiredResponse(final String host) {
    HttpMessageResponse response = new HttpMessageResponse();
    response.setStartLine(HTTP_407_RESPONSE);
    response.addHeader(HttpMessage.HEADER_PROXY_AUTHENTICATE,
        BASIC_AUTHENTICATION_METHOD);
    logger.debug("Failed to authenticate for " + host);
    return response;
  }

  /**
   * Extracts credentials from the request.
   * 
   * @param request - input Request object to parse
   * @return - decoded credentials extracted from the request, null if not found
   */
  private String getCredentials(final HttpMessageRequest request) {
    List<String> values =
        request.getHeaderValues(HttpMessage.HEADER_PROXY_AUTHORIZATION);
    if (values == null) {
      return null;
    }
    String decodedCredentials = null;
    Iterator<String> it = values.iterator();
    while (it.hasNext()) {
      String authString = it.next();
      if (!authString.startsWith(BASIC_AUTHENTICATION_METHOD)) {
        // For now only use basic authentication
        continue;
      }
      String credentialsString =
          authString.substring(BASIC_AUTHENTICATION_METHOD.length() + 1);
      // Default encoding
      byte[] decodedBytes = Base64.decodeBase64(credentialsString.getBytes());
      decodedCredentials = new String(decodedBytes);
      break; // take only the first one
    }
    return decodedCredentials;
  }

  /**
   * Checks whether url is unprotected by the password.
   * 
   * @param url - url to check
   * @return true if the given url is unprotected , otherwise false
   */
  private boolean checkUnprotectedUrl(final String url) {
    return checkUrl(DEFAULT_USER + CREDENTIALS_USER_PASSWORD_SEPARATOR
        + UNPROTECTED_URL_TOKEN, url);
  }

  /**
   * Checks whether given url is authorized.
   * 
   * @param credentials - username/password
   * @param url - the url to check
   * @return true is given credentials authorize given url, otherwise false
   */
  private boolean checkUrl(final String credentials, final String url) {
    if (credentials == null) {
      return false;
    }
    List<Pattern> patterns = urlPatternsMap.get(credentials);
    if (patterns == null) {
      return false;
    }
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Parses the property object and inserts rules in the urlPatternMap.
   * 
   * @param props - property object containing all authentication rules
   */
  private void parseProperties(final Properties props) {
    Set<String> keys = props.stringPropertyNames();
    for (String key : keys) {
      String patterns = props.getProperty(key);
      if (key.indexOf(PROPERTIES_USER_PASSWORD_SEPARATOR) > 0) {
        // username is specified in properties
        key = key.replace(PROPERTIES_USER_PASSWORD_SEPARATOR,
            CREDENTIALS_USER_PASSWORD_SEPARATOR);
      } else {
        // use default username
        key = DEFAULT_USER + CREDENTIALS_USER_PASSWORD_SEPARATOR + key;
      }
      String[] parsedPatterns = patterns.split(URL_PATTERNS_SEPARATOR);

      List<Pattern> urlPatterns = new ArrayList<Pattern>();
      for (int i = 0; i < parsedPatterns.length; i++) {
        urlPatterns.add(Pattern.compile(parsedPatterns[i],
            Pattern.CASE_INSENSITIVE));
      }
      urlPatternsMap.put(key, urlPatterns);
    }
  }
}
