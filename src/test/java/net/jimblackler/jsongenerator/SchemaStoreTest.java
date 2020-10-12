package net.jimblackler.jsongenerator;

import static net.jimblackler.jsongenerator.ReaderUtils.getLines;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class SchemaStoreTest {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() {
    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");

    getLines(SuiteTest.class.getResourceAsStream(testDir.toString()), resource -> {
      String other = resource + ".json";
      Path testSchema = schemaPath.resolve(other);
      URL resource1 = SchemaStoreTest.class.getResource(testSchema.toString());
      if (resource1 == null) {
        return;
      }

      try {
        testsOut.add(dynamicTest(other, resource1.toURI(), () -> {
          SchemaStore schemaStore = new SchemaStore();
          Schema schema = schemaStore.loadSchema(resource1);
          System.out.println("Schema:");
          System.out.println(JsonUtils.toString(schema));
          System.out.println();
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
          }, schemaStore, new Random(1)).generate(schema, 16);

          System.out.println(JsonUtils.toString(generated));
          new Validator().validate(schema, generated);
        }));
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    });

    return testsOut;
  }
}
