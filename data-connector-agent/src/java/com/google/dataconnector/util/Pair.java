/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.google.dataconnector.util;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.gdata.util.common.base.Nullable;

/**
 * A pair.
 * 
 * A pair is an immutable sequence with two elements (or components). The types of both elements
 * can be different. Null values are permitted.
 *
 * @param <A> the type of the first element 
 * @param <B> the type of the second element
 * 
 * @author kevinb@google.com (Kevin Bourrillion)
 * 
 */
public final class Pair<A, B> implements Serializable {

  private static final long serialVersionUID = 2327744049909665364L;

  private final A first;
  private final B second;

  /**
   * Creates a new pair containing the given elements in order.
   */
  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<A, B>(first, second);
  }

  /**
   * Constructs a pair with the given two elements.
   * @param first the first element
   * @param second the second element
   */
  public Pair(@Nullable A first, @Nullable B second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Returns the first element of this pair.
   * @return the first element
   */
  public A first() {
    return first;
  }

  /**
   * Returns the second element of this pair.
   * @return the second element
   */
  public B second() {
    return second;
  }

  /**
   * Returns the hash code for this pair.
   * @return the hash code value for this pair
   */
  @Override
  public int hashCode() {
    return (first != null ? 41 * first.hashCode() : 0) + (second != null ? second.hashCode() : 0); 
  }

  /**
   * Compares the specified object with this pair for equality. 
   * Returns {@code true} if and only if the specified object is also a pair and the
   * elements of both pairs are equal.
   * @param obj object to be compared for equality with this pair
   * @return {@code true} if the specified object is equal to this pair
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Pair) {
            Pair<?, ?> other = (Pair<?, ?>) obj;
            return Objects.equal(first, other.first) && Objects.equal(second, other.second);
    }
    return false;
  }

  /**
   * Returns a string representation of this pair.
   * @return the string representation of this pair
   */
  @Override
  public String toString() {
    return "Pair[" + first + ", " + second + "]";
  }
}
