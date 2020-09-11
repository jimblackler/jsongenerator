package net.jimblacker.jsongenerator;

import static net.jimblacker.jsongenerator.CollectionUtils.randomElement;
import static net.jimblacker.jsongenerator.Fixer.fixUp;
import static net.jimblacker.jsongenerator.StringUtils.randomString;
import static net.jimblacker.jsongenerator.ValueUtils.getDouble;
import static net.jimblacker.jsongenerator.ValueUtils.getLong;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {
  private static final int MAX_STRING_LENGTH = 60;
  private static final int MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH = 50;
  private final Configuration configuration;
  private final Random random;
  private final PatternReverser patternReverser;
  private final Schema anySchema;

  public Generator(Configuration configuration, SchemaStore schemaStore, Random random)
      throws GenerationException {
    this.configuration = configuration;
    this.random = random;
    anySchema = schemaStore.loadSchema(true);
    try {
      patternReverser = new PatternReverser();
    } catch (IOException e) {
      throw new GenerationException(e);
    }
  }

  public Schema getAnySchema() {
    return anySchema;
  }

  public Object generate(Schema schema, int maxTreeSize)
      throws JsonGeneratorException, MissingPathException {
    if (schema.isFalse()) {
      throw new JsonGeneratorException(
          schema.getUri() + " : nothing can validate against a false schema");
    }

    Object object;
    if (configuration.isGenerateMinimal()) {
      object = 1;
    } else {
      object = generateUnvalidated(schema, maxTreeSize);
    }

    return fixUp(schema, object, this, random, patternReverser);
  }

  private Object generateUnvalidated(Schema schema, int maxTreeSize) {
    Object _const = schema.getConst();
    if (_const != null) {
      return _const;
    }

    // Naive.
    Collection<Schema> allOf = schema.getAllOf();
    if (allOf != null && !allOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, allOf), maxTreeSize);
    }

    // Naive.
    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, anyOf), maxTreeSize);
    }

    // Naive.
    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null && !oneOf.isEmpty()) {
      return generateUnvalidated(randomElement(random, oneOf), maxTreeSize);
    }

    // Naive.
    Schema then = schema.getThen();
    if (then != null) {
      return generateUnvalidated(then, maxTreeSize);
    }

    Collection<String> types = configuration.isPedanticTypes() ? schema.getNonProhibitedTypes()
                                                               : schema.getInferredTypes();

    types = new HashSet<>(types);

    if (maxTreeSize < 1) {
      if (types.size() > 1) {
        types.remove("array");
      }
      if (types.size() > 1) {
        types.remove("object");
      }
    }

    Schema contains = schema.getContains();
    if (contains != null && contains.isFalse()) {
      // Special case for when 'contains' is a false schema. We can't generate an array.
      types = new HashSet<>(schema.getNonProhibitedTypes());
      types.remove("array");
    }

    Collection<String> explicitTypes = schema.getExplicitTypes();
    if (!configuration.isGenerateNulls()
        && (explicitTypes == null || !explicitTypes.contains("null"))) {
      // We remove null if the configuration demands it, but not if the schema calls for it.
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
        long minimum = getLong(schema.getMinimum(), Integer.MIN_VALUE);
        long maximum = getLong(schema.getMaximum(), Integer.MAX_VALUE - 1);
        if (!schema.getExclusiveMaximumBoolean()) {
          maximum++;
        }
        if (maximum - minimum == 0) {
          throw new IllegalStateException();
        }
        long value = Math.abs(random.nextLong()) % (maximum - minimum) + minimum;
        if (schema.getMultipleOf() != null) {
          int multipleOf = schema.getMultipleOf().intValue();
          long multiples = value / multipleOf;
          value = multiples * multipleOf;
        }

        return value;
      }
      case "string": {
        long minLength = getLong(schema.getMinLength(), 0);
        long maxLength = getLong(schema.getMaxLength(), Integer.MAX_VALUE - 1);
        if (!schema.getExclusiveMaximumBoolean()) {
          maxLength++;
        }
        long useMaxLength = Math.min(maxLength, minLength + MAX_STRING_LENGTH);
        Ecma262Pattern pattern1 = schema.getPattern();
        String pattern = pattern1 == null ? null : pattern1.toString();
        if (pattern != null) {
          return patternReverser.reverse(pattern, random);
        }

        return randomString(random, (int) minLength, (int) useMaxLength);
      }
      case "array": {
        List<Schema> schemas = new ArrayList<>();

        long minItems = getLong(schema.getMinItems(), 0);
        long maxItems = getLong(schema.getMaxItems(), Integer.MAX_VALUE);

        long length = random.nextInt(Math.max(maxTreeSize, 0) + 1);
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
          Object value = generateUnvalidated(schema1, (int) (0.7f * maxTreeSize));
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

        long minProperties = getLong(schema.getMinProperties(), 0);
        long maxProperties = getLong(schema.getMaxProperties(), Integer.MAX_VALUE);

        long length = random.nextInt(Math.max(maxTreeSize, 0) + 1);
        if (length < minProperties) {
          length = minProperties;
        }

        if (length > maxProperties) {
          length = maxProperties;
        }

        for (String property : requiredProperties) {
          Schema schema1 = properties.get(property);
          if (schema1 == null) {
            schema1 = anySchema;
          }
          schemas.put(property, schema1);
        }

        List<String> nonRequired = new ArrayList<>(properties.keySet());
        nonRequired.removeAll(requiredProperties);
        Collections.shuffle(nonRequired, random);

        for (String property : nonRequired) {
          if (schemas.size() >= length) {
            break;
          }
          if (random.nextFloat() < configuration.nonRequiredPropertyChance()) {
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

              String str = patternReverser.reverse(pattern, random);
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
            propertyName = randomString(random, 1, MAX_ADDITIONAL_PROPERTIES_KEY_LENGTH);
          }

          if (additionalProperties == null) {
            schemas.put(propertyName, anySchema);
            if (random.nextBoolean()) {
              // We don't want to generate too many of these, as they look like gibberish, and one
              // or two makes the point.
              break;
            }
          } else {
            schemas.put(propertyName, additionalProperties);
            continue;
          }
        }

        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Schema> entries : schemas.entrySet()) {
          String key = entries.getKey();
          if (jsonObject.has(key)) {
            throw new IllegalStateException();
          }
          int size = jsonObject.keySet().size();
          jsonObject.put(key, generateUnvalidated(entries.getValue(), (int) (0.7f * maxTreeSize)));
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
}
