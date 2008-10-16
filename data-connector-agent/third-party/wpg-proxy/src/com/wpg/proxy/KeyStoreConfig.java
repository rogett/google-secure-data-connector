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
 * Configuration parameters for the KeyStore.
 * 
 */
public class KeyStoreConfig {

  /** KeyStore file name including full path. */
  private String keystoreFilename;

  /** The password for the KeyStore. */
  private String keystorePassword;

  /** The alias for the key entry in the KeyStore. */
  private String keyAlias;

  /**
   * Constructs the config with the given params.
   * 
   * @param fileName - KeyStore file name including full path
   * @param pass - The password for the KeyStore
   * @param alias - The alias for the key entry in the KeyStore
   */
  public KeyStoreConfig(final String fileName, final String pass,
      final String alias) {
    keystoreFilename = fileName;
    keystorePassword = pass;
    keyAlias = alias;
  }

  /**
   * @return KeyStore filename
   */
  public final String getKeystoreFileName() {
    return keystoreFilename;
  }

  /**
   * @return KeyStore password
   */
  public final String getKeystorePassword() {
    return keystorePassword;
  }

  /**
   * @return alias for the key entry
   */
  public final String getAlias() {
    return keyAlias;
  }
}
