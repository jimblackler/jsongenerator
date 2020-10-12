package net.jimblackler.jsongenerator;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  private static Collection<DynamicNode> scan(
      Collection<Path> testDirs, Path remotes, URI metaSchema) {
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
                JSONArray data = (JSONArray) DocumentUtils.loadJson(inputStream1);
                for (int idx = 0; idx != data.length(); idx++) {
                  JSONObject testSet = data.getJSONObject(idx);
                  if (!testSet.has("schema")) {
                    continue; // ever happens?
                  }

                  JSONArray tests1 = testSet.getJSONArray("tests");
                  boolean anyValid = false;
                  for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
                    JSONObject test = tests1.getJSONObject(idx2);
                    if (test.getBoolean("valid")) {
                      anyValid = true;
                      break;
                    }
                  }

                  if (!anyValid) {
                    continue;
                  }
                  nodes.add(dynamicTest(testSet.getString("description"), () -> {
                    Object schema = testSet.get("schema");
                    if (schema instanceof JSONObject) {
                      ((JSONObject) schema).put("$schema", metaSchema.toString());
                    }

                    System.out.println("Schema:");
                    System.out.println(JsonUtils.toString(schema));
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
                      public float nonRequiredPropertyChance() {
                        return 0.5f;
                      }
                    }, schemaStore, new Random(1)).generate(schema1, 16);

                    if (generated instanceof JSONObject) {
                      System.out.println(((JSONObject) generated).toString(2));
                    } else if (generated instanceof JSONArray) {
                      System.out.println(((JSONArray) generated).toString(2));
                    } else {
                      System.out.println(generated);
                    }
                    new Validator().validate(schema1, generated);

                    // Does it also pass Everit?
                    if (false)
                      if (schema instanceof JSONObject) {
                        Schema everitSchema = SchemaLoader.load((JSONObject) schema, url -> {
                          url = url.replace("http://localhost:1234",
                              SuiteTest.class.getResource(remotes.toString()).toString());
                          try {
                            return new URL(url).openStream();
                          } catch (IOException e1) {
                            throw new UncheckedIOException(e1);
                          }
                        });

                        try {
                          everitSchema.validate(generated);
                        } catch (ValidationException ex) {
                          System.out.println(ex.toJSON());
                        } catch (Exception e1) {
                          fail(e1);
                        }
                      }
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
