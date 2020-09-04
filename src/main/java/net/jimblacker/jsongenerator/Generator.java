package net.jimblacker.jsongenerator;

import com.mifmif.common.regex.Generex;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.ObjectSchema;
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
    try {
      schema.validate(object);
      return object;
    } catch (ValidationException e) {
      e.printStackTrace();
      return null;
    }
  }

  private Object generateUnvalidated(Schema schema) {
    JSONObject jsonObject = new JSONObject();
    ObjectSchema objectSchema = (ObjectSchema) schema;

    Collection<Schema> oneOf = objectSchema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generate(randomElement(random, oneOf));
    }

    Collection<Schema> anyOf = objectSchema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generate(randomElement(random, anyOf));
    }

    Set<String> types = objectSchema.getTypes();
    if (types.isEmpty()) {
      throw new IllegalStateException("No types");
    }
    String type = randomElement(random, types);

    switch (type) {
      case "boolean": {
        return random.nextBoolean();
      }
      case "number": {
        double minimum = getDouble(objectSchema.getMinimum(), -Double.MAX_VALUE);
        double maximum = getDouble(objectSchema.getMaximum(), Double.MAX_VALUE);
        double value = random.nextDouble() % (maximum - minimum) + minimum;
        if (objectSchema.getMultipleOf() != null) {
          double multipleOf = objectSchema.getMultipleOf().doubleValue();
          int multiples = (int) (value / multipleOf);
          value = multiples * multipleOf;
        }

        return value;
      }
      case "integer": {
        long minimum = getInt(objectSchema.getMinimum(), Integer.MIN_VALUE);
        long maximum = getInt(objectSchema.getMaximum(), Integer.MAX_VALUE);
        long value = Math.abs(random.nextLong()) % (maximum - minimum) + minimum;
        if (objectSchema.getMultipleOf() != null) {
          int multipleOf = objectSchema.getMultipleOf().intValue();
          long multiples = value / multipleOf;
          value = multiples * multipleOf;
        }

        return value;
      }
      case "string": {
        List<Object> enums = objectSchema.getEnums();
        if (enums != null) {
          return randomElement(random, enums);
        }
        int minLength = getInt(objectSchema.getMinLength(), 0);
        int maxLength = getInt(objectSchema.getMaxLength(), Integer.MAX_VALUE);
        int useMaxLength = Math.min(maxLength, minLength + MAX_STRING_LENGTH);
        int length = random.nextInt(useMaxLength - minLength) + minLength + 1;
        Ecma262Pattern pattern1 = objectSchema.getPattern();
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
        int minItems = getInt(objectSchema.getMinItems(), 0);
        int maxItems = getInt(objectSchema.getMaxItems(), Integer.MAX_VALUE);
        int useMaxItems = Math.min(maxItems, minItems + MAX_ARRAY_LENGTH);
        int length = random.nextInt(useMaxItems - minItems) + minItems + 1;
        Collection<Schema> items = objectSchema.getItems();
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
        Map<String, Schema> properties = objectSchema.getProperties();
        Collection<String> requiredProperties = objectSchema.getRequiredProperties();
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
          String property = entry.getKey();
          if (requiredProperties.contains(property) || random.nextBoolean()) {
            Schema schema1 = entry.getValue();
            Object object = generateUnvalidated(schema1);
            try {
              schema1.validate(object);

              jsonObject.put(property, object);
            } catch (ValidationException e) {
              e.printStackTrace();
            }
          }
        }

        Collection<Ecma262Pattern> patternPropertiesPatterns =
            objectSchema.getPatternPropertiesPatterns();
        if (patternPropertiesPatterns != null && !patternPropertiesPatterns.isEmpty()) {
          Collection<Schema> patternPropertiesSchema = objectSchema.getPatternPropertiesSchema();
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

        Schema additionalProperties = objectSchema.getAdditionalProperties();
        if (additionalProperties != null) {
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
