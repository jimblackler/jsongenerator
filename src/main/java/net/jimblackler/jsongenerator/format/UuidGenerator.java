package net.jimblackler.jsongenerator.format;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

public class UuidGenerator implements StringGenerator {

  @Override
  public String get(Random random) {
    return UUID.nameUUIDFromBytes(String.valueOf(random.nextLong()).getBytes(StandardCharsets.UTF_8)).toString();
  }

}
