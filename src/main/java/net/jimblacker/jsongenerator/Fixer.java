package net.jimblacker.jsongenerator;

import static net.jimblacker.jsongenerator.ValueUtils.getInt;
import static net.jimblackler.jsonschemafriend.Validator.validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.jimblackler.jsonschemafriend.AnyOfError;
import net.jimblackler.jsonschemafriend.ConstError;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.EnumError;
import net.jimblackler.jsonschemafriend.ExclusiveMaximumError;
import net.jimblackler.jsonschemafriend.ExclusiveMinimumError;
import net.jimblackler.jsonschemafriend.MaximumError;
import net.jimblackler.jsonschemafriend.MinItemsError;
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
import net.jimblackler.jsonschemafriend.ValidationError;
import org.json.JSONArray;
import org.json.JSONObject;

public class Fixer {
  static Object fixUp(Schema schema, Object object, Generator generator, Random random) {
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
          Object fixed = fix(objectToFix, error, generator, random);
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

  private static Object fix(
      Object object, ValidationError error, Generator generator, Random random) {
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
      Schema schema1 = error.getSchema().getProperties().get(property);
      JSONObject jsonObject = (JSONObject) object;
      try {
        Object value = generator.generate(schema1, 50);
        jsonObject.put(property, value);
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
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
      return object;
    }

    if (error instanceof MinItemsError) {
      Schema additionalItemsSchema = schema.getAdditionalItems();
      JSONArray array = (JSONArray) object;

      try {
        if (additionalItemsSchema == null) {
          array.put(generator.generate(generator.getAnySchema(), 50));
        } else {
          array.put(generator.generate(additionalItemsSchema, 50));
        }
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
    }

    if (error instanceof OneOfError) {
      try {
        return generator.generate(generator.getAnySchema(), 5);
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
    }

    if (error instanceof TypeError) {
      // TODO: just regenerate?
      TypeError error1 = (TypeError) error;
      Collection<String> expectedTypes = error1.getExpectedTypes();
      String type = CollectionUtils.randomElement(random, expectedTypes);
      switch (type) {
        case "array":
          return new JSONArray();
        case "boolean":
          return false;
        case "integer":
          return 0;
        case "null":
          return null;
        case "number":
          return 0;
        case "object":
          return new JSONObject();
        case "string":
          return "";
        default:
      }
    }

    if (error instanceof PatternError) {
      Ecma262Pattern pattern1 = schema.getPattern();
      return PatternReverser.reverse(pattern1.toString(), getInt(schema.getMinLength(), 0),
          getInt(schema.getMaxLength(), Integer.MAX_VALUE), random);
    }

    return object;
  }
}
