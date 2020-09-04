package net.jimblacker.jsongenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 20;
  private static final int MAX_ARRAY_LENGTH = 5;
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

    String type = randomElement(random, objectSchema.getTypes());

    switch (type) {
      case "integer": {
        int multipleOf = getInt(objectSchema.getMultipleOf(), 1);
        int minimum = getInt(objectSchema.getMinimum(), 0);
        int maximum = getInt(objectSchema.getMaximum(), Integer.MAX_VALUE);
        int multiples = (maximum - minimum) / multipleOf;
        return random.nextInt(multiples) * multipleOf;
      }
      case "string": {
        int minLength = getInt(objectSchema.getMinLength(), 0);
        int maxLength = getInt(objectSchema.getMaxLength(), Integer.MAX_VALUE);
        int useMaxLength = Math.min(maxLength, minLength + MAX_STRING_LENGTH);
        int length = random.nextInt(useMaxLength - minLength) + minLength + 1;
        StringBuilder stringBuilder = new StringBuilder();
        for (int idx = 0; idx != length; idx++) {
          stringBuilder.append((char) (random.nextInt(128 - 32) + 32));
        }
        return stringBuilder.toString();
      }
      case "array": {
        JSONArray jsonArray = new JSONArray();
        int minItems = getInt(objectSchema.getMinItems(), 0);
        int maxItems = getInt(objectSchema.getMaxItems(), Integer.MAX_VALUE);
        int useMaxItems = Math.min(maxItems, minItems + MAX_STRING_LENGTH);
        int length = random.nextInt(useMaxItems - minItems) + minItems + 1;
        Collection<Schema> items = objectSchema.getItems();
        for (int idx = 0; idx != length; idx++) {
          jsonArray.put(generate(random, randomElement(random, items)));
        }
        return jsonArray;
      }
      case "object": {
        Map<String, Schema> properties = objectSchema.getProperties();
        for (String requiredProperty : objectSchema.getRequiredProperties()) {
          Schema schema1 = properties.get(requiredProperty);
          Object object = generateUnvalidated(random, schema1);
          try {
            schema1.validate(object);
            jsonObject.put(requiredProperty, object);
          } catch (ValidationException e) {
            e.printStackTrace();
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
}
