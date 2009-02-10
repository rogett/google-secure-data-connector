package com.google.dataconnector.registration.v2;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations for Guice Providers.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationV2Annotations {
  
  /**
   * Provider for "my hostname" parameter.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @BindingAnnotation
  public @interface MyHostname {}
  
 }
