package net.jimblackler.jsongenerator.format;

import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class DateTimeGeneratorTest {

  private static final DateTimeGenerator DATE_TIME_GENERATOR = new DateTimeGenerator();
  private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  @Test
  public void test() {
    assertNotNull(DateTimeFormatter.ISO_DATE_TIME.parse(DATE_TIME_GENERATOR.get(RANDOM)));
    assertNotNull(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DATE_TIME_GENERATOR.get(RANDOM)));
  }

}
