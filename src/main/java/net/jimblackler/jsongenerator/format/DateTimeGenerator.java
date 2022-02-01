package net.jimblackler.jsongenerator.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

public class DateTimeGenerator extends BaseStringGenerator implements StringGenerator {

  private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  private final long maxEpochMillis = Instant.now().getEpochSecond() * 1000;

  public DateTimeGenerator(Random random) {
    super(random);
  }

  @Override
  public String get() {
    final long randomEpochMillis = random.nextLong(maxEpochMillis);
    return FORMAT.format(Date.from(Instant.ofEpochMilli(randomEpochMillis)));
  }

  public static void main(String[] args) {
    DateTimeGenerator generator = new DateTimeGenerator(new Random());
    for (int i = 0; i < 10; ++i) {
      System.out.println(generator.get());
    }
  }

}
