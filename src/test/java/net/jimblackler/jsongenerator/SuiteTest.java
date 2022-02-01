package net.jimblackler.jsongenerator;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  private static Collection<DynamicNode> scan(
      Collection<Path> testDirs, Path remotes, URI metaSchema) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    ObjectReader objectReader = objectMapper.reader();
    Collection<DynamicNode> allFileTests = new ArrayList<>();

    for (Path testDir : testDirs) {
      try (InputStream inputStream = SuiteTest.class.getResourceAsStream(testDir.toString());
           BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (resource.endsWith(".json")) {
            try {
              Collection<DynamicNode> nodes = new ArrayList<>();
              try (InputStream inputStream1 =
                       SuiteTest.class.getResourceAsStream(testDir.resolve(resource).toString())) {
                List<Object> data = objectReader.readValue(inputStream1, List.class);
                for (int idx = 0; idx != data.size(); idx++) {
                  Map<String, Object> testSet = (Map<String, Object>) data.get(idx);
                  if (!testSet.containsKey("schema")) {
                    continue; // ever happens?
                  }

                  List<Object> tests1 = (List<Object>) testSet.get("tests");
                  boolean anyValid = false;
                  for (int idx2 = 0; idx2 != tests1.size(); idx2++) {
                    Map<String, Object> test = (Map<String, Object>) tests1.get(idx2);
                    if (Boolean.TRUE.equals(test.get("valid"))) {
                      anyValid = true;
                      break;
                    }
                  }

                  if (!anyValid) {
                    continue;
                  }
                  nodes.add(dynamicTest((String) testSet.get("description"), () -> {
                    Object schema = testSet.get("schema");
                    if (schema instanceof Map) {
                      ((Map<String, Object>) schema).put("$schema", metaSchema.toString());
                    }

                    System.out.println("Schema:");
                    System.out.println(objectWriter.writeValueAsString(schema));
                    System.out.println();

                    SchemaStore schemaStore = new SchemaStore(in
                        -> URI.create(in.toString().replace("http://localhost:1234",
                            SuiteTest.class.getResource(remotes.toString()).toString())));
                    net.jimblackler.jsonschemafriend.Schema schema1 =
                        schemaStore.loadSchema(schema);

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
                    }, schemaStore, new Random(1)).generate(schema1, 16);

                    System.out.println(objectWriter.writeValueAsString(generated));
                    new Validator().validate(schema1, generated);
                  }));
                }
              }
              allFileTests.add(dynamicContainer(resource, nodes));
            } catch (IOException e) {
              throw new IllegalStateException("Problem with " + resource, e);
            }
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return allFileTests;
  }

  private static Collection<DynamicNode> test(String set, String metaSchema) {
    Path suite = FILE_SYSTEM.getPath("/suites").resolve("JSON-Schema-Test-Suite");
    Path tests = suite.resolve("tests").resolve(set);
    Path optional = tests.resolve("optional");
    List<Path> paths = new ArrayList<>();
    paths.add(tests);
    paths.add(optional);
    paths.add(optional.resolve("format"));
    Path remotes = suite.resolve("remotes");
    return scan(paths, remotes, URI.create(metaSchema));
  }

  @TestFactory
  Collection<DynamicNode> own() {
    Path path = FILE_SYSTEM.getPath("/suites");
    Path own = path.resolve("own");
    Collection<Path> testDirs = new HashSet<>();
    testDirs.add(own);
    Path remotes = path.resolve("own_remotes");
    URI metaSchema = URI.create("http://json-schema.org/draft-07/schema#");
    return scan(testDirs, remotes, metaSchema);
  }

  @TestFactory
  Collection<DynamicNode> draft3() {
    return test("draft3", "http://json-schema.org/draft-03/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft4() {
    return test("draft4", "http://json-schema.org/draft-04/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft6() {
    return test("draft6", "http://json-schema.org/draft-06/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft7() {
    return test("draft7", "http://json-schema.org/draft-07/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft2019_09() {
    return test("draft2019-09", "https://json-schema.org/draft/2019-09/schema");
  }
}
