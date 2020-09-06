package net.jimblacker.jsongenerator;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 60;
  private static final int MAX_ARRAY_LENGTH = 20;
  private static final int MAXIMUM_TARGET_PROPERTIES = 10;
  private static final int MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH = 20;
  private final Configuration configuration;
  private final Random random;
  private final Schema anySchema;

  public Generator(Configuration configuration, SchemaStore schemaStore, Random random)
      throws GenerationException {
    this.configuration = configuration;
    this.random = random;
    anySchema = schemaStore.loadSchema(true);
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

  public Object generate(Schema schema, int maxTreeSize) {
    int seed = random.nextInt();
    random.setSeed(seed);

    Object object = generateUnvalidated(schema, maxTreeSize);
    int tries = 1;
    for (int idx = 0; idx != tries; idx++) {
      Collection<ValidationError> errors = new ArrayList<>();
      schema.validate(object, errors::add);
      if (!errors.isEmpty()) {
        if (idx == 0) {
          System.out.println("Object:");
          if (object instanceof JSONObject) {
            System.out.println(((JSONObject) object).toString(2));
          } else if (object instanceof JSONArray) {
            System.out.println(((JSONArray) object).toString(2));
          } else {
            System.out.println(object);
          }
          System.out.println();
          for (ValidationError error : errors) {
            System.out.println(error);
          }
          random.setSeed(seed);
          Object object2 = generateUnvalidated(schema, maxTreeSize);
        }
        if (idx == tries - 1) {
          //          for (ValidationError error : errors) {
          //            System.out.println(error);
          //          }
          return null;
        }
      }
      return object;
    }
    return null;
  }

  private Object generateUnvalidated(Schema schema, int maxTreeSize) {
    // Naive.
    Collection<Schema> allOf = schema.getAllOf();
    if (allOf != null && !allOf.isEmpty()) {
      return generate(randomElement(random, allOf), maxTreeSize - 1);
    }

    // Naive.
    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generate(randomElement(random, anyOf), maxTreeSize - 1);
    }

    // Naive.
    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generate(randomElement(random, oneOf), maxTreeSize - 1);
    }

    Collection<String> types =
        configuration.isExploitAmbiguousTypes() ? schema.getExplicitTypes() : schema.getTypes();

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
        double minimum = getDouble(
            schema.getMinimum(), getDouble(schema.getExclusiveMinimum(), -Double.MAX_VALUE));
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
          return PatternReverser.reverse(pattern, minLength, maxLength, random);
        }

        return randomString(length);
      }
      case "array": {
        List<Schema> schemas = new ArrayList<>();

        int minItems = getInt(schema.getMinItems(), 0);
        int maxItems = getInt(schema.getMaxItems(), Integer.MAX_VALUE);

        int length = random.nextInt(maxTreeSize);
        if (length < minItems) {
          length = minItems;
        }

        if (length > maxItems) {
          length = maxItems;
        }

        Collection<Schema> itemsTuple = schema.getItemsTuple();
        Schema items = schema.getItems();
        Schema additionalItemsSchema = schema.getAdditionalItems();
        Iterator<Schema> it = itemsTuple == null ? null : itemsTuple.iterator();
        while (schemas.size() < length) {
          if (items != null) {
            schemas.add(items);
          } else if (it != null && it.hasNext()) {
            schemas.add(it.next());
          } else if (additionalItemsSchema != null) {
            if (additionalItemsSchema.isFalse()) {
              break;
            }
            schemas.add(additionalItemsSchema);
          } else {
            schemas.add(anySchema);
          }
        }

        JSONArray jsonArray = new JSONArray();
        for (Schema schema1 : schemas) {
          jsonArray.put(generate(schema1, (maxTreeSize - 1) / length));
        }
        return jsonArray;
      }
      case "object": {
        Map<String, Schema> schemas = new HashMap<>();
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
            schemas.put(property, schema1);
          }
        }

        Collection<Ecma262Pattern> patternPropertiesPatterns =
            schema.getPatternPropertiesPatterns();
        Schema additionalProperties = schema.getAdditionalProperties();

        int minProperties = getInt(schema.getMinProperties(), 0);
        int maxProperties = getInt(schema.getMaxProperties(), Integer.MAX_VALUE);

        int length = random.nextInt(maxTreeSize);
        if (length < minProperties) {
          length = minProperties;
        }

        if (length > minProperties) {
          length = minProperties;
        }

        int targetProperties = random.nextInt(MAXIMUM_TARGET_PROPERTIES);
        while (schemas.keySet().size() < targetProperties) {
          if (patternPropertiesPatterns != null && !patternPropertiesPatterns.isEmpty()) {
            Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
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

            String str = PatternReverser.reverse(pattern, 1, Integer.MAX_VALUE, random);
            if (schemas.containsKey(str)) {
              // Probably an inflexible pattern. Let's just give up.
              break;
            }
            schemas.put(str, it1.next());

          } else if (additionalProperties != null && !additionalProperties.isFalse()) {
            schemas.put(randomString(MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH), additionalProperties);
          } else {
            break;
          }
        }

        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Schema> entries : schemas.entrySet()) {
          jsonObject.put(
              entries.getKey(), generate(entries.getValue(), (maxTreeSize - 1) / length));
        }
        return jsonObject;
      }
      case "null": {
        return null;
      }
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

  private String randomString(int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) random.nextInt());
    }
    // We don't aim to handle strings that won't survive URL encoding with standard methods.
    String urlEncoded = URLEncoder.encode(stringBuilder.toString());
    return URLDecoder.decode(urlEncoded);
  }

  public interface Configuration {
    boolean isExploitAmbiguousTypes();
  }
}
