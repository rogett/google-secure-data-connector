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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Used for testing, this trust manager allows all certs.  Only use for testing!
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class TrustAllTrustManager implements TrustManager, X509TrustManager {

  public X509Certificate[] getAcceptedIssuers() {
    return null;
  }
  public boolean isServerTrusted(X509Certificate[] certs) {
    return true;
  }
  public boolean isClientTrusted(X509Certificate[] certs) {
    return true;
  } 
  public void checkServerTrusted(X509Certificate[] certs, String authType) 
      throws CertificateException {
    return;
  }
  public void checkClientTrusted(X509Certificate[] certs, String authType) 
      throws CertificateException {
    return;
  }
}
