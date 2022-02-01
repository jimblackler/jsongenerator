package net.jimblackler.jsongenerator;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class FakeSchemaTest {
  @TestFactory
  Collection<DynamicNode> all() throws GenerationException {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    Collection<DynamicNode> testsOut = new ArrayList<>();

    Map<String, Object> schemaObject = new LinkedHashMap<>();
    String metaSchema = "http://json-schema.org/draft-07/schema#";
    schemaObject.put("$ref", metaSchema);
    schemaObject.put("$schema", metaSchema);
    Schema schema = new SchemaStore().loadSchema(schemaObject);

    for (int attempt = 100; attempt != 200; attempt++) {
      int finalAttempt = attempt;
      testsOut.add(dynamicTest("" + attempt, () -> {
        SchemaStore schemaStore = new SchemaStore();

        Generator generator = new Generator(new Configuration() {
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
          public boolean isGenerateAdditionalProperties() {
            return false;
          }

          @Override
          public boolean useRomanCharsOnly() {
            return false;
          }

          @Override
          public float nonRequiredPropertyChance() {
            return 0.5f;
          }
        }, schemaStore, new Random(finalAttempt));
        Object generated = generator.generate(schema, 16);

        System.out.println(objectWriter.writeValueAsString(generated));
        new Validator().validate(schema, generated);

        if (generated instanceof Map) {
          ((Map<String, Object>) generated).put("$schema", metaSchema);
        }

        // Now convert to schema.
        Schema schema2 = new SchemaStore().loadSchema(generated);

        try {
          Object generated2 = generator.generate(schema2, 16);

          new Validator().validate(schema2, generated2);
        } catch (JsonGeneratorException | OutOfMemoryError e) {
          e.printStackTrace();
        }
      }));
    }
    return testsOut;
  }
}
