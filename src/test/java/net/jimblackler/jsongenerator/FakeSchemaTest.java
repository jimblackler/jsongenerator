package net.jimblackler.jsongenerator;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class FakeSchemaTest {
  @TestFactory
  Collection<DynamicNode> all() throws GenerationException {
    Collection<DynamicNode> testsOut = new ArrayList<>();

    JSONObject schemaObject = new JSONObject();
    schemaObject.put("$ref", "http://json-schema.org/draft-07/schema#");
    schemaObject.put("$schema", "http://json-schema.org/draft-07/schema#");
    Schema schema = new SchemaStore().loadSchema(schemaObject);

    for (int attempt = 0; attempt != 20; attempt++) {
      int finalAttempt = attempt;
      testsOut.add(dynamicTest("" + attempt, () -> {
        SchemaStore schemaStore = new SchemaStore();

        Object generated = new Generator(new Configuration() {
          @Override
          public boolean isPedanticTypes() {
            return false;
          }

          @Override
          public boolean isGenerateNulls() {
            return false;
          }

          @Override
          public boolean isGenerateMinimal() {
            return false;
          }

          @Override
          public float nonRequiredPropertyChance() {
            return 0.5f;
          }
        }, schemaStore, new Random(finalAttempt)).generate(schema, 16);

        System.out.println(JsonUtils.toString(generated));
        new Validator().validate(schema, generated);
      }));
    }
    return testsOut;
  }
}
