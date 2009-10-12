package com.google.dataconnector.util;

/**
 * A class to aid in the process of mocking time related things.
 *
 * @author mtp@google.com (Matt T. Proud)
 *
 */
public class ClockUtil {
  /**
   * This method models {@link System#currentTimeMillis()}.
   *
   * @return The time in milliseconds.
   */
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
