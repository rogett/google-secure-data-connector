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

/**
 * Interface for a user defined HTTP Message Executor Its main purpose is to
 * provide opportunity for the proxy to send back response to the client without
 * contacting the target host. E.g for proxy authorization request (Http code
 * 407 )
 * 
 */
public interface HttpMessageExecutor {

  /**
   * Executes request and produces response to be send back to the client. It
   * should return null if request to be submitted to the target host.
   */
  public HttpMessageResponse executeRequest(HttpMessageRequest request);
}
