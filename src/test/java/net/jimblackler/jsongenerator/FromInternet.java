package net.jimblackler.jsongenerator;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONArray;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class FromInternet {
  private static final URI DEFAULT_METASCHEMA =
      URI.create("http://json-schema.org/draft-07/schema#");

  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Collection<DynamicTest> testsOut = new ArrayList<>();

    try (
        InputStream inputStream = FromInternet.class.getResourceAsStream("/internetSchemas.json")) {
      JSONArray array = new JSONArray(streamToString(inputStream));
      for (int idx = 0; idx != array.length(); idx++) {
        String str = array.getString(idx);
        testsOut.add(DynamicTest.dynamicTest(str, () -> {
          URI uri = URI.create(str);
          System.out.println(uri);
          System.out.println();
          SchemaStore schemaStore = new SchemaStore();
          Schema schema = schemaStore.loadSchema(uri);
          System.out.println("Schema:");
          System.out.println(JsonUtils.toString(schema.getSchemaObject()));
          System.out.println();

          Object object = new Generator(new Configuration() {
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
          }, schemaStore, new Random(1)).generate(schema, 20);
          System.out.println("Data:");
          System.out.println(JsonUtils.toString(object));
          System.out.println();
          new Validator().validate(schema, object);
        }));
      }
    }
    return testsOut;
  }
}
