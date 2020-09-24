package net.jimblackler.jsongenerator;

public class ValueUtils {
  static long getLong(Number number, long _default) {
    if (number == null) {
      return _default;
    }
    return number.longValue();
  }

  static double getDouble(Number number, double _default) {
    if (number == null) {
      return _default;
    }
    return number.doubleValue();
  }
}
