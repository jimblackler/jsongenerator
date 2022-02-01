package net.jimblackler.jsongenerator.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

public class DateTimeGenerator extends BaseStringGenerator implements StringGenerator {

  private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private final long maxEpochMillis = Instant.now().getEpochSecond() * 1000;

  public DateTimeGenerator(Random random) {
    super(random);
  }

  @Override
  public String get() {
    final long randomEpochMillis = random.nextLong(maxEpochMillis);
    // append Z manually to mark the time as UTC
    return FORMAT.format(Date.from(Instant.ofEpochMilli(randomEpochMillis))) + "Z";
  }

}
