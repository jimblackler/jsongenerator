package net.jimblackler.jsongenerator;

import static net.jimblackler.jsongenerator.CollectionUtils.randomElement;
import static net.jimblackler.jsongenerator.Generator.FORMAT_REGEXES;
import static net.jimblackler.jsongenerator.StringUtils.randomString;
import static net.jimblackler.jsongenerator.ValueUtils.getLong;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.jimblackler.jsonschemafriend.AnyOfError;
import net.jimblackler.jsonschemafriend.ConstError;
import net.jimblackler.jsonschemafriend.DependencyError;
import net.jimblackler.jsonschemafriend.EnumError;
import net.jimblackler.jsonschemafriend.ExclusiveMaximumError;
import net.jimblackler.jsonschemafriend.ExclusiveMinimumError;
import net.jimblackler.jsonschemafriend.FalseSchemaError;
import net.jimblackler.jsonschemafriend.FormatError;
import net.jimblackler.jsonschemafriend.MaxLengthError;
import net.jimblackler.jsonschemafriend.MaxPropertiesError;
import net.jimblackler.jsonschemafriend.MaximumError;
import net.jimblackler.jsonschemafriend.MinItemsError;
import net.jimblackler.jsonschemafriend.MinLengthError;
import net.jimblackler.jsonschemafriend.MinPropertiesError;
import net.jimblackler.jsonschemafriend.MinimumError;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.MultipleError;
import net.jimblackler.jsonschemafriend.NotError;
import net.jimblackler.jsonschemafriend.OneOfError;
import net.jimblackler.jsonschemafriend.PathUtils;
import net.jimblackler.jsonschemafriend.PatternError;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.TypeError;
import net.jimblackler.jsonschemafriend.UniqueItemsError;
import net.jimblackler.jsonschemafriend.ValidationError;
import net.jimblackler.jsonschemafriend.Validator;

public class Fixer {
  static Object fixUp(Schema schema, Object object, Generator generator, Random random,
      PatternReverser patternReverser, boolean useRomanCharsOnly) throws JsonGeneratorException {
    int attempt = 0;
    Set<String> considered = new HashSet<>();
    while (true) {
      Collection<ValidationError> errors = new ArrayList<>();
      new Validator().validate(schema, object, errors::add);

      if (errors.isEmpty()) {
        break;
      }

      attempt++;

      if (!considered.add(object.toString())) {
        break;
      }
      List<ValidationError> errorList = new ArrayList<>(errors);
      while (!errorList.isEmpty()) {
        ValidationError error = errorList.remove(0);
        URI uri = error.getUri();
        String path = uri.getRawFragment();
        if (error instanceof FalseSchemaError && path != null && !path.isEmpty()) {
          try {
            PathUtils.deleteAtPath(object, path);
          } catch (MissingPathException e) {
            e.printStackTrace();
          }
          continue;
        }

        if (error instanceof AnyOfError) {
          AnyOfError error1 = (AnyOfError) error;
          List<List<ValidationError>> allErrors = error1.getAllErrors();
          int i = random.nextInt(allErrors.size());
          // Pick one at random and fix the object to match it.
          errorList.addAll(allErrors.get(i));
          continue;
        }

        if (error instanceof OneOfError) {
          OneOfError error1 = (OneOfError) error;
          List<List<ValidationError>> allErrors = error1.getAllErrors();
          int i = random.nextInt(allErrors.size());
          // Pick one at random and fix the object to match it.
          errorList.addAll(allErrors.get(i));
          continue;
        }

        try {
          Object objectToFix = Validator.getObject(object, uri);
          Object fixed = fix(objectToFix, error, generator, random, patternReverser, useRomanCharsOnly);
          if (objectToFix == object) {
            object = fixed;
          } else if (objectToFix != fixed) {
            object = PathUtils.modifyAtPath(object, path, fixed);
          }
        } catch (MissingPathException e) {
          e.printStackTrace();
        }

        break; // TODO: reconsider this.
      }
    }
    return object;
  }

  private static Object fix(Object object, ValidationError error, Generator generator,
      Random random, PatternReverser patternReverser, boolean useRomanCharsOnly) throws JsonGeneratorException {
    Schema schema = error.getSchema();
    if (error instanceof ConstError) {
      return schema.getConst();
    }

    if (error instanceof EnumError) {
      List<Object> enums = schema.getEnums();
      return enums.get(random.nextInt(enums.size()));
    }

    if (error instanceof MissingPropertyError) {
      MissingPropertyError error1 = (MissingPropertyError) error;
      String property = error1.getProperty();
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      jsonObject.put(property, 0);
      return object;
    }

    if (error instanceof DependencyError) {
      DependencyError error1 = (DependencyError) error;
      String property = error1.getDependency();
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      jsonObject.put(property, 0);
      return object;
    }

    if (error instanceof MultipleError) {
      Number multiple = schema.getMultipleOf();
      Number objectAsNumber = (Number) object;
      double v = multiple.doubleValue();
      int multiples = (int) (objectAsNumber.doubleValue() / v);
      return multiples * v;
    }

    if (error instanceof MinimumError) {
      return schema.getMinimum();
    }

    if (error instanceof MaximumError) {
      return schema.getMaximum();
    }

    if (error instanceof ExclusiveMinimumError) {
      // TODO: do something more sophisticated.
      return schema.getExclusiveMinimum().floatValue() + 0.1f;
    }

    if (error instanceof ExclusiveMaximumError) {
      // TODO: do something more sophisticated.
      return schema.getExclusiveMaximum().floatValue() - 0.1f;
    }

    if (error instanceof NotError) {
      return 0;
    }

    if (error instanceof MinItemsError) {
      List<Object> array = (List<Object>) object;
      while (array.size() < schema.getMinItems().intValue()) {
        array.add(0);
      }
      return array;
    }

    if (error instanceof TypeError) {
      // TODO: just regenerate?
      TypeError error1 = (TypeError) error;
      Collection<String> expectedTypes = error1.getExpectedTypes();
      String type = randomElement(random, expectedTypes);
      switch (type) {
        case "array":
          return new LinkedHashMap<String, Object>();
        case "boolean":
          return false;
        case "integer":
          return 0;
        case "null":
          return null;
        case "number":
          return 0.1;
        case "object":
          return new LinkedHashMap<String, Object>();
        case "string":
          return "";
        default:
      }
    }

    if (error instanceof PatternError) {
      String pattern1 = schema.getPattern();
      Random r2 = new Random(random.nextInt(10)); // Limited number to help detect lost causes.
      return patternReverser.reverse(pattern1, r2);
    }

    if (error instanceof MinLengthError || error instanceof MaxLengthError) {
      long minLength = getLong(schema.getMinLength(), 0);
      return randomString(random, (int) minLength, (int) minLength + 5, useRomanCharsOnly);
    }

    if (error instanceof MinPropertiesError) {
      int minProperties = schema.getMinProperties().intValue();
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      Map<String, Schema> properties = schema.getProperties();
      for (Map.Entry<String, Schema> entry : properties.entrySet()) {
        jsonObject.put(entry.getKey(), 0);
      }
      while (jsonObject.size() < minProperties) {
        Collection<String> patternPropertiesPatterns = schema.getPatternPropertiesPatterns();
        Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
        if (patternPropertiesPatterns.isEmpty()) {
          jsonObject.put(randomString(random, 1, 20, useRomanCharsOnly), 0);
        } else {
          int index = random.nextInt(patternPropertiesPatterns.size());
          Iterator<String> it0 = patternPropertiesPatterns.iterator();
          Iterator<Schema> it1 = patternPropertiesSchema.iterator();
          while (index > 0) {
            it0.next();
            it1.next();
            index--;
          }
          Schema schema1 = it1.next();
          if (!schema1.isFalse()) {
            String pattern = it0.next();
            String str = patternReverser.reverse(pattern, random);
            jsonObject.put(str, 0);
          }
        }
      }
      return jsonObject;
    }

    if (error instanceof MaxPropertiesError) {
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      int maxProperties = schema.getMaxProperties().intValue();
      while (jsonObject.size() > maxProperties) {
        String propertyName = randomElement(random, jsonObject.keySet());
        jsonObject.remove(propertyName);
      }
      return jsonObject;
    }

    if (error instanceof UniqueItemsError) {
      List<Object> jsonArray = (List<Object>) object;
      Collection<Object> out = new ArrayList<>();
      Collection<String> included = new HashSet<>();
      for (int idx = 0; idx != jsonArray.size(); idx++) {
        Object obj = jsonArray.get(idx);
        if (included.add(obj.toString())) {
          out.add(obj);
        }
      }
      return out;
    }

    if (error instanceof FormatError) {
      String pattern0 = FORMAT_REGEXES.get(schema.getFormat());
      if (pattern0 != null) {
        return patternReverser.reverse(pattern0, random);
      }
    }
    throw new JsonGeneratorException("Can't fix " + error);
  }
}
