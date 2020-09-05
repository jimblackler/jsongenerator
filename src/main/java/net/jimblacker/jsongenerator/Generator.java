package net.jimblacker.jsongenerator;

import com.mifmif.common.regex.Generex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 60;
  private static final int MAX_ARRAY_LENGTH = 20;
  private static final int MAX_PATTERN_PROPERTIES = 5;
  private static final int MAX_ADDITIONAL_PROPERTIES = 5;
  private static final int MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH = 20;

  private final Random random;
  private final Schema anySchema;

  public Generator(SchemaStore schemaStore, Random random) throws GenerationException {
    this.random = random;
    anySchema = schemaStore.loadSchema(true);
  }

  public Object generate(Schema schema) {
    Object object = generateUnvalidated(schema);
    int tries = 5;
    for (int idx = 0; idx != tries; idx++) {
      try {
        schema.validate(object);
        return object;
      } catch (ValidationException e) {
        if (idx == tries - 1) {
          e.printStackTrace();
          return null;
        }
      }
    }
    return null;
  }

  private Object generateUnvalidated(Schema schema) {
    JSONObject jsonObject = new JSONObject();

    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generate(randomElement(random, oneOf));
    }

    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generate(randomElement(random, anyOf));
    }

    Set<String> types = schema.getTypes();
    if (types.isEmpty()) {
      throw new IllegalStateException("No types");
    }
    String type = randomElement(random, types);
    List<Object> enums = schema.getEnums();
    if (enums != null) {
      return randomElement(random, enums);
    }
    switch (type) {
      case "boolean": {
        return random.nextBoolean();
      }
      case "number": {
        double minimum = getDouble(schema.getMinimum(), -Double.MAX_VALUE);
        double maximum = getDouble(schema.getMaximum(), Double.MAX_VALUE);
        double value = random.nextDouble() % (maximum - minimum) + minimum;
        if (schema.getMultipleOf() != null) {
          double multipleOf = schema.getMultipleOf().doubleValue();
          int multiples = (int) (value / multipleOf);
          value = multiples * multipleOf;
        }

        return value;
      }
      case "integer": {
        long minimum = getInt(schema.getMinimum(), Integer.MIN_VALUE);
        long maximum = getInt(schema.getMaximum(), Integer.MAX_VALUE);
        long value = Math.abs(random.nextLong()) % (maximum - minimum) + minimum;
        if (schema.getMultipleOf() != null) {
          int multipleOf = schema.getMultipleOf().intValue();
          long multiples = value / multipleOf;
          value = multiples * multipleOf;
        }

        return value;
      }
      case "string": {
        int minLength = getInt(schema.getMinLength(), 0);
        int maxLength = getInt(schema.getMaxLength(), Integer.MAX_VALUE);
        int useMaxLength = Math.min(maxLength, minLength + MAX_STRING_LENGTH);
        int length = random.nextInt(useMaxLength - minLength) + minLength + 1;
        Ecma262Pattern pattern1 = schema.getPattern();
        String pattern = pattern1 == null ? null : pattern1.toString();
        if (pattern != null) {
          if (pattern.startsWith("^")) {
            pattern = pattern.substring("^".length());
          }
          Generex generex = new Generex(pattern, random);
          return generex.random(minLength, maxLength);
        }

        return randomString(length);
      }
      case "array": {
        JSONArray jsonArray = new JSONArray();
        int minItems = getInt(schema.getMinItems(), 0);
        int maxItems = getInt(schema.getMaxItems(), Integer.MAX_VALUE);
        int useMaxItems = Math.min(maxItems, minItems + MAX_ARRAY_LENGTH);
        int length = random.nextInt(useMaxItems - minItems) + minItems + 1;
        Collection<Schema> items = schema.getItems();
        for (int idx = 0; idx != length; idx++) {
          if (items.isEmpty()) {
            jsonArray.put(generate(anySchema));
          } else {
            jsonArray.put(generate(randomElement(random, items)));
          }
        }
        return jsonArray;
      }
      case "object": {
        Map<String, Schema> properties = schema.getProperties();
        Collection<String> requiredProperties = schema.getRequiredProperties();
        Collection<String> allProperties = new ArrayList<>();
        allProperties.addAll(requiredProperties);
        allProperties.addAll(properties.keySet());
        for (String property : allProperties) {
          if (requiredProperties.contains(property) || random.nextBoolean()) {
            Schema schema1 = properties.get(property);
            if (schema1 == null) {
              schema1 = anySchema;
            }
            jsonObject.put(property, generate(schema1));
          }
        }

        Collection<Ecma262Pattern> patternPropertiesPatterns =
            schema.getPatternPropertiesPatterns();
        if (patternPropertiesPatterns != null && !patternPropertiesPatterns.isEmpty()) {
          Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
          int addPatternProperties = random.nextInt(MAX_PATTERN_PROPERTIES);
          for (int idx = 0; idx != addPatternProperties; idx++) {
            int index = random.nextInt(patternPropertiesPatterns.size());
            Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
            Iterator<Schema> it1 = patternPropertiesSchema.iterator();
            while (index > 0) {
              it0.next();
              it1.next();
              index--;
            }
            String pattern = it0.next().toString();

            if (pattern.startsWith("^")) {
              pattern = pattern.substring("^".length());
            }
            Generex generex = new Generex(pattern, random);
            jsonObject.put(generex.random(), generate(it1.next()));
          }
        }

        Schema additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties != null && !additionalProperties.isFalse()) {
          int addPatternProperties = random.nextInt(MAX_ADDITIONAL_PROPERTIES);
          for (int idx = 0; idx != addPatternProperties; idx++) {
            jsonObject.put(
                randomString(MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH), generate(additionalProperties));
          }
        }

        return jsonObject;
      }
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

  private String randomString(int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) (random.nextInt(128 - 32) + 32));
    }
    return stringBuilder.toString();
  }

  public static <T> T randomElement(Random random, Collection<T> collection) {
    int size = collection.size();
    if (size == 0) {
      throw new IllegalStateException();
    }
    int selection = random.nextInt(size);
    for (T element : collection) {
      if (selection == 0) {
        return element;
      }
      selection--;
    }
    throw new IllegalStateException();
  }

  private static int getInt(Number number, int _default) {
    if (number == null) {
      return _default;
    }
    return number.intValue();
  }

  private static double getDouble(Number number, double _default) {
    if (number == null) {
      return _default;
    }
    return number.doubleValue();
  }
}
