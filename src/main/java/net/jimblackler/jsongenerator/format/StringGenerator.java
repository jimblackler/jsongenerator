package net.jimblackler.jsongenerator.format;

import java.util.Random;

public interface StringGenerator {

  /**
   * @return random string with the given random generator.
   */
  String get(Random random);

}
