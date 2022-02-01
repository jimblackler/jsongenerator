package net.jimblackler.jsongenerator.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

public class DateGenerator extends BaseStringGenerator implements StringGenerator {

  private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private final long maxEpochSecond = Instant.now().getEpochSecond();

  public DateGenerator(Random random) {
    super(random);
  }

  @Override
  public String get() {
    final long randomEpochSecond = random.nextLong(maxEpochSecond);
    return FORMAT.format(Date.from(Instant.ofEpochSecond(randomEpochSecond)));
  }

}
