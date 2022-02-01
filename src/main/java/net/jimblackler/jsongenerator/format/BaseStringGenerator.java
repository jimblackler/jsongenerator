package net.jimblackler.jsongenerator.format;

import java.util.Random;

public abstract class BaseStringGenerator implements StringGenerator {

  protected final Random random;

  protected BaseStringGenerator(Random random) {
    this.random = random;
  }

}
