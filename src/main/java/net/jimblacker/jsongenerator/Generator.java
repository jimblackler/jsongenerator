package net.jimblacker.jsongenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.mifmif.common.regex.Generex;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 60;
  private static final int MAX_ARRAY_LENGTH = 20;
  public static Object generate(Random random, Schema schema) {
    Object object = generateUnvalidated(random, schema);
    try {
      schema.validate(object);
      return object;
    } catch (ValidationException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static Object generateUnvalidated(Random random, Schema schema) {
    JSONObject jsonObject = new JSONObject();
    ObjectSchema objectSchema = (ObjectSchema) schema;

    Collection<Schema> oneOf = objectSchema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generateUnvalidated(random, randomElement(random, oneOf));
    }

    Collection<Schema> anyOf = objectSchema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generateUnvalidated(random, randomElement(random, anyOf));
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
        int minimum = getInt(objectSchema.getMinimum(), -Integer.MAX_VALUE);
        int maximum = getInt(objectSchema.getMaximum(), Integer.MAX_VALUE);
        int value = random.nextInt(maximum - minimum) + minimum;
        if (objectSchema.getMultipleOf() != null) {
          int multipleOf = objectSchema.getMultipleOf().intValue();
          int multiples = value / multipleOf;
          value = multiples * multipleOf;
        }

        return value;
      }
      case "string": {
        List<Object> enums = objectSchema.getEnums();
        if (enums == null) {

          int minLength = getInt(objectSchema.getMinLength(), 0);
          int maxLength = getInt(objectSchema.getMaxLength(), Integer.MAX_VALUE);
          int useMaxLength = Math.min(maxLength, minLength + MAX_STRING_LENGTH);
          int length = random.nextInt(useMaxLength - minLength) + minLength + 1;
          String pattern = objectSchema.getPattern();
          if (pattern != null) {
            if (pattern.startsWith("^")) {
              pattern = pattern.substring("^".length());
            }
            Generex generex = new Generex(pattern, random);
            return generex.random(minLength, maxLength);
          }

          StringBuilder stringBuilder = new StringBuilder();
          for (int idx = 0; idx != length; idx++) {
            stringBuilder.append((char) (random.nextInt(128 - 32) + 32));
          }
          return stringBuilder.toString();
        } else {
          return randomElement(random, enums);
        }
      }
      case "array": {
        JSONArray jsonArray = new JSONArray();
        int minItems = getInt(objectSchema.getMinItems(), 0);
        int maxItems = getInt(objectSchema.getMaxItems(), Integer.MAX_VALUE);
        int useMaxItems = Math.min(maxItems, minItems + MAX_ARRAY_LENGTH);
        int length = random.nextInt(useMaxItems - minItems) + minItems + 1;
        Collection<Schema> items = objectSchema.getItems();
        for (int idx = 0; idx != length; idx++) {
          jsonArray.put(generate(random, randomElement(random, items)));
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
            Object object = generateUnvalidated(random, schema1);
            try {
              schema1.validate(object);

              jsonObject.put(property, object);
            } catch (ValidationException e) {
              e.printStackTrace();
            }
          }
        }

        return jsonObject;
      }
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

  public static <T> T randomElement(Random random, Collection<T> collection) {
    int selection = random.nextInt(collection.size());
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
