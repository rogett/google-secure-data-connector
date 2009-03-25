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

/**
 * Exception for errors during protocol communications with Secure Link Server.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceException extends Exception {

  /**
   * Creates the exception with the specified error message.
   * 
   * @param msg the error message.
   */
  public ResourceException(String msg) {
    super(msg);
  }
  
  public ResourceException(Throwable cause) {
    super(cause);
  }
  
  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
