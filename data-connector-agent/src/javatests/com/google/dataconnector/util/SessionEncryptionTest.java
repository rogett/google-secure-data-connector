// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.dataconnector.util;

import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import java.util.Random;

/**
 * Tests for the session level encryption / decryption utilities.
 * 
 * @author dchung@google.com (David Chung)
 *
 */
public class SessionEncryptionTest extends TestCase {

  public void testEncryptDecrypt() throws Exception {
   
    SessionEncryption se = new SessionEncryption();  // Default algo and generated. key.
    
    // Create some random bytes
    Random rand = new Random(System.currentTimeMillis());
    
    byte[] buff = new byte[1024];
    rand.nextBytes(buff);
    
    ByteString input = ByteString.copyFrom(buff);
    ByteString encrypted = se.encrypt(input);
    ByteString decrypted = se.decrypt(encrypted);
    
    assertNotSame(input, encrypted);
    assertEquals(input, decrypted);
    
    assertFalse(same(buff, encrypted.toByteArray()));
    assertTrue(same(buff, decrypted.toByteArray()));
  }

  private boolean same(byte[] a, byte[] b) {
    if (a.length != b.length) return false;
    boolean same = true;
    for (int i = 0; i < a.length && same; ++i) {
      same = a[i] == b[i];
    }
    return same;
  }
}
