package net.jimblacker.jsongenerator;

import static net.jimblackler.jsonschemafriend.Validator.validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.jimblackler.jsonschemafriend.AnyOfValidationError;
import net.jimblackler.jsonschemafriend.BelowMinItemsValidationError;
import net.jimblackler.jsonschemafriend.ConstMismatchError;
import net.jimblackler.jsonschemafriend.GreaterThanMaximumError;
import net.jimblackler.jsonschemafriend.LessThanMinimumError;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.NotAMultipleError;
import net.jimblackler.jsonschemafriend.NotValidationError;
import net.jimblackler.jsonschemafriend.OneOfValidationError;
import net.jimblackler.jsonschemafriend.PathUtils;
import net.jimblackler.jsonschemafriend.Schema;
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
          Object fixed = fix(objectToFix, error, generator);
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

  private static Object fix(Object object, ValidationError error, Generator generator) {
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
        Object value = generator.generate(schema, 50);
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
          array.put(generator.generate(generator.getAnySchema(), 50));
        } else {
          array.put(generator.generate(additionalItemsSchema, 50));
        }
      } catch (JsonGeneratorException e) {
        e.printStackTrace();
      }
    }

    return object;
  }
}
