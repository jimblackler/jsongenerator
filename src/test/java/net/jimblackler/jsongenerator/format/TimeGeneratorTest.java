package net.jimblackler.jsongenerator.format;

import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class TimeGeneratorTest {

  private static final TimeGenerator TIME_GENERATOR = new TimeGenerator();
  private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  @Test
  public void test() {
    assertNotNull(DateTimeFormatter.ISO_TIME.parse(TIME_GENERATOR.get(RANDOM)));
    assertNotNull(DateTimeFormatter.ISO_OFFSET_TIME.parse(TIME_GENERATOR.get(RANDOM)));
  }

}
