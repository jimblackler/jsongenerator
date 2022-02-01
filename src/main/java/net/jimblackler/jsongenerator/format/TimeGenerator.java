package net.jimblackler.jsongenerator.format;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Random;

/**
 * Generate a random UTC time.
 */
public class TimeGenerator implements StringGenerator {

  private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder().appendPattern("HH:mm:ss.SSS").toFormatter();

  protected TimeGenerator() {

  }

  @Override
  public String get(Random random) {
    // append Z manually to mark the time as UTC
    return LocalTime.MIDNIGHT.plusSeconds(random.nextLong()).format(FORMAT) + "Z";
  }

}
