package net.jimblackler.jsongenerator.format;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class UuidGeneratorTest {

  private static final UuidGenerator UUID_GENERATOR = new UuidGenerator();
  private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  @Test
  public void test() {
    assertNotNull(UUID.fromString(UUID_GENERATOR.get(RANDOM)));
  }

}
