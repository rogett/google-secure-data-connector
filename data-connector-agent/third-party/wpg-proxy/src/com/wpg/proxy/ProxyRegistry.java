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

import java.util.Vector;

/**
 * A static data container that keeps references to every
 * HttpMessageHandler/Processor that must be called by the Proxy for each
 * message. It also containes the SSL key information.
 */
public class ProxyRegistry {
  private boolean statusBrowser = false;
  private String keyfile = null;
  private char[] keystorePass = null;
  private char[] keystoreKeysPass = null;
  private final Vector<HttpMessageRequestProcessor> requestProcessors =
      new Vector<HttpMessageRequestProcessor>();
  private final Vector<HttpMessageHandler> handlers =
      new Vector<HttpMessageHandler>();
  private final Vector<HttpMessageResponseProcessor> responseProcessors =
      new Vector<HttpMessageResponseProcessor>();
  private HttpMessageExecutor messageExecutor = null;

  /** is the status browser capability enabled? */
  public boolean isStatusBrowserEnabled() {
    return statusBrowser;
  }

  /** enable or dissable the status browser capability */
  public void enableStatusBrowser(boolean enable) {
    statusBrowser = enable;
  }

  /**
   * Add a new handler to receive incomming http responses, as well as the
   * request
   */
  public void addHandler(HttpMessageHandler hml) {
    handlers.addElement(hml);
  }

  /** Removes a response handler */
  public boolean removeHandler(HttpMessageHandler hml) {
    return handlers.remove(hml);
  }

  /** Get the list of request handlers */
  protected Vector<HttpMessageHandler> getHandlers() {
    return handlers;
  }

  /** Add a new request processor */
  public void addRequestProcessor(HttpMessageRequestProcessor hmreqp) {
    requestProcessors.addElement(hmreqp);
  }

  /** Removes a response processor */
  public boolean removeRequestProcessor(HttpMessageRequestProcessor hmreqp) {
    return requestProcessors.remove(hmreqp);
  }

  /** Get the list of request processors */
  protected Vector<HttpMessageRequestProcessor> getRequestProcessors() {
    return requestProcessors;
  }

  /** Add a new response processor */
  public void addResponseProcessor(HttpMessageResponseProcessor hmp) {
    responseProcessors.addElement(hmp);
  }

  /** Removes a response processor */
  public boolean removeResponseProcessor(HttpMessageResponseProcessor hmp) {
    return responseProcessors.remove(hmp);
  }

  /** Get the list of response processors */
  protected Vector<HttpMessageResponseProcessor> getResponseProcessors() {
    return responseProcessors;
  }

  /** Set message executor */
  public void setMessageExecutor(HttpMessageExecutor executor) {
    messageExecutor = executor;
  }

  /** Get message executor */
  public HttpMessageExecutor getMessageExecutor() {
    return messageExecutor;
  }

  /** Set Keystore File Name */
  protected void setKeystoreFilename(String s) {
    keyfile = s;
  }

  /** Set Keystore password */
  protected void setKeystorePassword(char[] c) {
    keystorePass = c;
  }

  /** Set Keystore keys password */
  protected void setKeystoreKeysPassword(char[] c) {
    keystoreKeysPass = c;
  }

  /** Get Keystore File Name */
  protected String getKeystoreFilename() {
    return keyfile;
  }

  /** Get Keystore password */
  protected char[] getKeystorePassword() {
    return keystorePass;
  }

  /** Get Keystore keys password */
  protected char[] getKeystoreKeysPassword() {
    return keystoreKeysPass;
  }
}
