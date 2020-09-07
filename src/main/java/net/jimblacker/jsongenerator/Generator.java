package net.jimblacker.jsongenerator;

import static net.jimblackler.jsonschemafriend.Validator.validate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.AnyOfValidationError;
import net.jimblackler.jsonschemafriend.BelowMinItemsValidationError;
import net.jimblackler.jsonschemafriend.ConstMismatchError;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.GreaterThanMaximumError;
import net.jimblackler.jsonschemafriend.LessThanMinimumError;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.NotAMultipleError;
import net.jimblackler.jsonschemafriend.NotValidationError;
import net.jimblackler.jsonschemafriend.OneOfValidationError;
import net.jimblackler.jsonschemafriend.PathUtils;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 60;
  private static final int MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH = 30;
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

  public Object generate(Schema schema, int maxTreeSize) throws JsonGeneratorException {
    if (schema.isFalse()) {
      throw new JsonGeneratorException(
          schema.getUri() + " : nothing can validate against a false schema");
    }
    int seed = random.nextInt();
    random.setSeed(seed);

    Object object = generateUnvalidated(schema, maxTreeSize);
    int attempts = 0;
    while (true) {
      Collection<ValidationError> errors = new ArrayList<>();
      validate(schema, object, errors::add);
      attempts++;
      if (errors.isEmpty()) {
        break;
      }
      if (attempts == 5) {
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
        break;
      }
      List<ValidationError> errorList = new ArrayList<>(errors);
      while (!errorList.isEmpty()) {
        ValidationError error = errorList.remove(0);

        if (error instanceof AnyOfValidationError) {
          AnyOfValidationError error1 = (AnyOfValidationError) error;
          List<List<ValidationError>> allErrors = error1.getAllErrors();
          int i = random.nextInt(allErrors.size());
          // Pick one at random and fix the object to match it.
          errorList.addAll(allErrors.get(i));
          continue;
        }

        if (error instanceof OneOfValidationError) {
          OneOfValidationError error1 = (OneOfValidationError) error;
          if (error1.getNumberPassed() == 0) {
            List<List<ValidationError>> allErrors = error1.getAllErrors();
            int i = random.nextInt(allErrors.size());
            // Pick one at random and fix the object to match it.
            errorList.addAll(allErrors.get(i));
            continue;
          }
        }

        try {
          Object objectToFix = PathUtils.fetchFromPath(object, error.getUri().getRawFragment());
          Object fixed = fix(objectToFix, error);
          if (objectToFix == object) {
            object = fixed;
          } else if (objectToFix != fixed) {
            PathUtils.modifyAtPath(object, error.getUri().getRawFragment(), fixed);
          }
        } catch (MissingPathException e) {
          e.printStackTrace();
        }
      }
    }

    return object;
  }

  private Object fix(Object object, ValidationError error) {
    if (error instanceof ConstMismatchError) {
      ConstMismatchError error1 = (ConstMismatchError) error;
      return error1.getConst();
    }
    if (error instanceof MissingPropertyError) {
      MissingPropertyError error1 = (MissingPropertyError) error;
      String property = error1.getProperty();
      Schema schema = error.getSchema().getProperties().get(property);
      JSONObject jsonObject = (JSONObject) object;
      try {
        Object value = generate(schema, 50);
        jsonObject.put(property, value);
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
      return object;
    }
    if (error instanceof NotAMultipleError) {
      NotAMultipleError error1 = (NotAMultipleError) error;
      Number multiple = error1.getMultiple();
      Number objectAsNumber = (Number) object;
      double v = multiple.doubleValue();
      int multiples = (int) (objectAsNumber.doubleValue() / v);
      return multiples * v;
    }

    if (error instanceof LessThanMinimumError) {
      LessThanMinimumError error1 = (LessThanMinimumError) error;
      return error1.getMinimum();
    }

    if (error instanceof GreaterThanMaximumError) {
      GreaterThanMaximumError error1 = (GreaterThanMaximumError) error;
      return error1.getMaximum();
    }

    if (error instanceof NotValidationError) {
      return object;
    }

    if (error instanceof BelowMinItemsValidationError) {
      BelowMinItemsValidationError error1 = (BelowMinItemsValidationError) error;
      Schema additionalItemsSchema = error.getSchema().getAdditionalItems();
      JSONArray array = (JSONArray) object;

      try {
        if (additionalItemsSchema == null) {
          array.put(generate(anySchema, 50));
        } else {
          array.put(generate(additionalItemsSchema, 50));
        }
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
    }

    return object;
  }

  private Object generateUnvalidated(Schema schema, int maxTreeSize) throws JsonGeneratorException {
    Object _const = schema.getConst();
    if (_const != null) {
      return _const;
    }

    // Naive.
    Collection<Schema> allOf = schema.getAllOf();
    if (allOf != null && !allOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, allOf), maxTreeSize - 1);
    }

    // Naive.
    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, anyOf), maxTreeSize - 1);
    }

    // Naive.
    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, oneOf), maxTreeSize - 1);
    }

    // Naive.
    Schema then = schema.getThen();
    if (then != null) {
      return generateUnvalidated(then, maxTreeSize - 1);
    }

    Collection<String> types =
        configuration.isPedanticTypes() ? schema.getNonProhibitedTypes() : schema.getTypes();

    Schema contains = schema.getContains();
    if (contains != null && contains.isFalse()) {
      types = new HashSet<>(schema.getNonProhibitedTypes());
      types.remove("array");
    }

    if (!configuration.isGenerateNulls()) {
      types = new HashSet<>(schema.getNonProhibitedTypes());
      types.remove("null");
    }

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

        int length = random.nextInt(random.nextInt(maxTreeSize + 1) + 1);
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

        while (schemas.size() < length || contains != null) {
          if (it != null && it.hasNext()) {
            Schema next = it.next();
            if (next.isFalse()) {
              break;
            }
            schemas.add(next);
            continue;
          }
          // Naive 'contains' handling.
          if (contains != null && random.nextBoolean()) {
            schemas.add(contains);
            contains = null;
            continue;
          }
          if (items != null) {
            if (items.isFalse()) {
              break;
            }
            schemas.add(items);
            continue;
          }
          if (additionalItemsSchema != null) {
            if (additionalItemsSchema.isFalse()) {
              break;
            }
            schemas.add(additionalItemsSchema);
            continue;
          }
          schemas.add(anySchema);
        }

        boolean uniqueItems = schema.isUniqueItems();
        Collection<Object> alreadyIncluded = new HashSet<>();
        JSONArray jsonArray = new JSONArray();
        for (Schema schema1 : schemas) {
          Object value = generateUnvalidated(schema1, (maxTreeSize - 1) / length);
          if (uniqueItems && !alreadyIncluded.add(value)) {
            continue;
          }
          jsonArray.put(value);
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
        Schema propertyNameSchema = schema.getPropertyNames();

        int minProperties = getInt(schema.getMinProperties(), 0);
        int maxProperties = getInt(schema.getMaxProperties(), Integer.MAX_VALUE);

        int length = random.nextInt(random.nextInt(maxTreeSize + 1) + 1);
        if (length < minProperties) {
          length = minProperties;
        }

        if (length > maxProperties) {
          length = maxProperties;
        }

        while (schemas.keySet().size() < length) {
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
            Schema schema1 = it1.next();
            if (!schema1.isFalse()) {
              String pattern = it0.next().toString();

              if (pattern.startsWith("^")) {
                pattern = pattern.substring("^".length());
              }

              String str = PatternReverser.reverse(pattern, 1, Integer.MAX_VALUE, random);
              if (schemas.containsKey(str)) {
                // Probably an inflexible pattern. Let's just give up.
                break;
              }
              schemas.put(str, schema1);
              continue;
            }
          }
          if (additionalProperties != null && additionalProperties.isFalse()) {
            break;
          }

          if (propertyNameSchema != null && propertyNameSchema.isFalse()) {
            break;
          }

          String propertyName = null;

          if (propertyNameSchema != null) {
            Object propertyNameObject = generateUnvalidated(schema.getPropertyNames(), 1);
            if (propertyNameObject instanceof String) {
              propertyName = (String) propertyNameObject;
            }
          }

          if (propertyName == null) {
            propertyName = randomString(MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH);
          }

          if (additionalProperties == null) {
            schemas.put(propertyName, anySchema);
          } else {
            schemas.put(propertyName, additionalProperties);
          }
        }

        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Schema> entries : schemas.entrySet()) {
          String key = entries.getKey();
          if (jsonObject.has(key)) {
            throw new IllegalStateException();
          }
          int size = jsonObject.keySet().size();
          jsonObject.put(key,
              generateUnvalidated(
                  entries.getValue(), (maxTreeSize - 1) / schemas.entrySet().size()));
          if (jsonObject.keySet().size() != size + 1) {
            throw new IllegalStateException();
          }
        }
        if (schemas.entrySet().size() != jsonObject.keySet().size()) {
          throw new IllegalStateException();
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
}
