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
package com.google.dataconnector.util;

/**
 * Super class for configuration exceptions.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AgentConfigurationException extends Exception {

  /**
   * Creates the exception with the specified error message.
   * 
   * @param msg the error message.
   */
  public AgentConfigurationException(String msg) {
    super(msg);
  }
  
  /**
   * Creates the exception with the specified underlying cause.
   * 
   * @param cause the underlying cause.
   */
  public AgentConfigurationException(Throwable cause) {
    super(cause);
  }
  
  /**
   * Creates the exception with the specified error message and cause.
   * 
   * @param msg the error message.
   * @param cause the underlying cause.
   */
  public AgentConfigurationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
