package net.jimblackler.jsongenerator.format;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class DateGeneratorTest {

  private static final DateGenerator DATE_GENERATOR = new DateGenerator();
  private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  @Test
  public void test() {
    assertNotNull(DateTimeFormatter.ISO_DATE.parse(DATE_GENERATOR.get(RANDOM)));
    assertNotNull(DateTimeFormatter.ISO_LOCAL_DATE.parse(DATE_GENERATOR.get(RANDOM)));
  }

}
