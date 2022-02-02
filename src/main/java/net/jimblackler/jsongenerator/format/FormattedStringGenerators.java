package net.jimblackler.jsongenerator.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class FormattedStringGenerators {

  private final Map<String, StringGenerator> generators;

  public FormattedStringGenerators() {
    this.generators = new HashMap<String, StringGenerator>() {{
      put("date", new DateGenerator());
      put("time", new TimeGenerator());
      put("date-time", new DateTimeGenerator());
      put("uuid", new UuidGenerator());
    }};
  }

  public Optional<String> get(String format, Random random) {
    if (format == null || !generators.containsKey(format)) {
      return Optional.empty();
    }
    return Optional.of(generators.get(format).get(random));
  }

}
