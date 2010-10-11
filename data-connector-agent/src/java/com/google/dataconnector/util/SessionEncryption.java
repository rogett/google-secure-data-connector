// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.dataconnector.util;

import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.gdata.util.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Support for session encryption of fetch and socket protocol. This class 
 * encapsulates the server-generated session encryption key and decrypts fetch
 * and socket messages from the server.
 * 
 * @author dchung@google.com (David Chung)
 *
 */
public class SessionEncryption {

  /**
   * Default algorithm for session encryption.  For local testing.  This is
   * determined by the server.
   */
  public static final String JCE_ALGO = "DES";

  private static final Logger LOG = Logger.getLogger(SessionEncryption.class);

  
  private final String sessionId;
  private final String algorithm;
  private final byte[] encryptionKey;
  private final Cipher decryptingCipher;
  private final Cipher encryptingCipher;
  
  SessionEncryption() 
  throws AgentConfigurationException {
    this(UUID.randomUUID().toString(), JCE_ALGO, newKeyBytes());
  }

  SessionEncryption(String sessionId, String algorithm, byte[] encryptionKey) 
    throws AgentConfigurationException {
    Preconditions.checkNotNull(encryptionKey);
    this.sessionId = sessionId;
    this.algorithm = algorithm;
    this.encryptionKey = encryptionKey;
    try {
      SecretKeySpec spec = new SecretKeySpec(this.encryptionKey, this.algorithm);
      this.decryptingCipher = Cipher.getInstance(this.algorithm);
      this.decryptingCipher.init(Cipher.DECRYPT_MODE, spec);
      this.encryptingCipher = Cipher.getInstance(this.algorithm);
      this.encryptingCipher.init(Cipher.ENCRYPT_MODE, spec);
    } catch (NoSuchAlgorithmException e) {
      LOG.fatal("No support for session encryption! ", e);
      throw new AgentConfigurationException(e);
    } catch (NoSuchPaddingException e) {
      LOG.fatal("No support for session encryption! ", e);
      throw new AgentConfigurationException(e);
    } catch (InvalidKeyException e) {
      LOG.fatal("No support for session encryption! ", e);
      throw new AgentConfigurationException(e);
    }
  }

  public String getSessionId() {
    return this.sessionId;
  }
  
  public String getAlgorithm() {
    return this.algorithm;
  }
  
  public byte[] getEncryptionKey() {
    return this.encryptionKey;
  }
  
  public ByteString encrypt(ByteString payload) 
    throws AgentConfigurationException {
    String message = "no encrypting cipher";
    if (encryptingCipher != null) {
      try {
        byte[] encrypted = encryptingCipher.doFinal(payload.toByteArray());
        return ByteString.copyFrom(encrypted);
      } catch (BadPaddingException e) {
        message = e.getMessage();
      } catch (IllegalBlockSizeException e) {
        message = e.getMessage();
      }
    } 
    throw new AgentConfigurationException("Unable to encrypt: " + message);
  }
  
  public ByteString decrypt(ByteString payload) 
    throws AgentConfigurationException {
    String message = "no decrypting cipher";
    if (decryptingCipher != null) {
      try {
        byte[] decrypted = decryptingCipher.doFinal(payload.toByteArray());
        return ByteString.copyFrom(decrypted);
      } catch (BadPaddingException e) {
        message = e.getMessage();
      } catch (IllegalBlockSizeException e) {
        message = e.getMessage();
      }
    } 
    throw new AgentConfigurationException("Unable to decrypt: " + message);
  }
  
  public interface Parse<M> {
    public M parse(ByteString s) throws InvalidProtocolBufferException;
  }
  
  public <M> M getFrom(FrameInfo frameInfo, Parse<M> p) 
    throws FramingException, InvalidProtocolBufferException {
    Preconditions.checkNotNull(frameInfo);
    String message = "";
    if (!frameInfo.hasPayload()) {
      message = ("No payload in received FrameInfo: " + frameInfo);
      throw new FramingException(message);
    }
    if (!frameInfo.hasSessionId()) {
      message = "Session id missing in fetch protocol.";
      throw new FramingException(message);
    }

    if (!frameInfo.getSessionId().equals(getSessionId())) {
      LOG.warn("Mismatched session id.");
      return null;
    }

    ByteString decrypted = null;
    try {
      long start = System.currentTimeMillis();
      decrypted = decrypt(frameInfo.getPayload());
      long dt = System.currentTimeMillis() - start;

      LOG.debug("Decrypted payload " + decrypted.size() + " bytes in " + dt + " msec.");
    } catch (AgentConfigurationException e) {
      LOG.warn("Cannot decrypt message for fetch protocol:" + e);
      return null;
    }
    return p.parse(decrypted);
  }

  public <M extends GeneratedMessage> FrameInfo toFrameInfo(FrameInfo.Type type, M reply) {
    ByteString encrypted = null;
    try {
      long start = System.currentTimeMillis();
      encrypted = encrypt(reply.toByteString());
      long dt = System.currentTimeMillis() - start;
      
      LOG.debug("Encrypted payload " + encrypted.size() + " bytes in " + dt + " msec.");
    } catch (AgentConfigurationException e) {
      LOG.warn("Cannot encrypt message for fetch protocol:", e);
      return null;
    }

    FrameInfo frame = (FrameInfo.newBuilder()
        .setType(type)
        .setPayload(encrypted)
        .setSessionId(getSessionId())
        .build());
    return frame;
  }

  /**
   * Generates a new key.
   * @return The generated key, in base64 encoding.
   */
  public static byte[] newKeyBytes() {
    try {
      KeyGenerator kg = KeyGenerator.getInstance(JCE_ALGO);
      return kg.generateKey().getEncoded();
    } catch (NoSuchAlgorithmException e) {
    }
    return null;
  }

}
