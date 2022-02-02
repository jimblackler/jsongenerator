package net.jimblackler.jsongenerator.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

/**
 * Generate a random UTC date time from 1970 to the future.
 */
public class DateTimeGenerator implements StringGenerator {

  private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private final long maxEpochMillis = Instant.now().getEpochSecond() * 1000 * 2;

  @Override
  public String get(Random random) {
    final long randomEpochMillis = (long) (random.nextDouble() * maxEpochMillis);
    // append Z manually to mark the time as UTC
    return FORMAT.format(Date.from(Instant.ofEpochMilli(randomEpochMillis))) + "Z";
  }

}
