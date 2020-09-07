package net.jimblackler.jsongenerator;

import static net.jimblackler.jsonschemafriend.Validator.validate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.jimblacker.jsongenerator.Configuration;
import net.jimblacker.jsongenerator.Generator;
import net.jimblackler.jsonschemafriend.DocumentSource;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  private static DynamicNode scan(Path testDir, Path remotes, URI metaSchema) {
    Collection<DynamicNode> allFileTests = new ArrayList<>();
    return dirScan(testDir, remotes, metaSchema, allFileTests);
  }

  private static DynamicNode dirScan(
      Path testDir, Path remotes, URI metaSchema, Collection<DynamicNode> allFileTests) {
    try (InputStream inputStream = SuiteTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;
      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          try {
            allFileTests.add(jsonTestFile(testDir, remotes, resource, metaSchema));
          } catch (IOException | SchemaException | URISyntaxException e) {
            throw new IllegalStateException("Problem with " + resource, e);
          }
        } else {
          if (false) // hack to exclude 'optional'
            dirScan(testDir.resolve(resource), remotes, metaSchema, allFileTests);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return dynamicContainer(testDir.toString(), allFileTests);
  }

  private static DynamicNode jsonTestFile(
      Path testDir, Path remotes, String resource, URI metaSchema)
      throws IOException, GenerationException, URISyntaxException, MissingPathException {
    Collection<DynamicNode> nodes = new ArrayList<>();
    try (InputStream inputStream =
             SuiteTest.class.getResourceAsStream(testDir.resolve(resource).toString())) {
      JSONArray data = (JSONArray) DocumentUtils.loadJson(inputStream);
      for (int idx = 0; idx != data.length(); idx++) {
        JSONObject testSet = data.getJSONObject(idx);
        if (!testSet.has("schema")) {
          continue; // ever happens?
        }
        nodes.add(singleSchemaTest(testSet, remotes, metaSchema));
      }
    }
    return dynamicContainer(resource, nodes);
  }

  private static DynamicNode singleSchemaTest(JSONObject testSet, Path remotes, URI metaSchema)
      throws URISyntaxException, GenerationException, MissingPathException {
    Collection<DynamicTest> ownTests = new ArrayList<>();
    Object schema = testSet.get("schema");
    URL resource = SuiteTest.class.getResource(remotes.toString());
    if (schema instanceof JSONObject) {
      JSONObject schema1 = (JSONObject) schema;
      schema1.put("$schema", metaSchema.toString());
    }

    DocumentSource documentSource = new DocumentSource(List.of(
        in -> URI.create(in.toString().replace("http://localhost:1234", resource.toString()))));
    URI local = new URI("memory", "local", null, null);
    documentSource.store(local, schema);
    SchemaStore schemaStore = new SchemaStore(documentSource);
    net.jimblackler.jsonschemafriend.Schema schema1 =
        schemaStore.loadSchema(local, URI.create("http://json-schema.org/draft-07/schema#"));

    JSONArray tests1 = testSet.getJSONArray("tests");
    boolean anyValid = false;
    for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
      JSONObject test = tests1.getJSONObject(idx2);
      if (test.getBoolean("valid")) {
        anyValid = true;
        break;
      }
    }

    if (anyValid) {
      ownTests.add(dynamicTest("Make JSON", () -> {
        System.out.println("Schema:");
        if (schema instanceof JSONObject) {
          System.out.println(((JSONObject) schema).toString(2));
        } else {
          System.out.println(schema);
        }
        System.out.println();

        Object generated = new Generator(new Configuration() {
          @Override
          public boolean isPedanticTypes() {
            return false;
          }

          @Override
          public boolean isGenerateNulls() {
            return true;
          }

          @Override
          public boolean isGenerateMinimal() {
            return false;
          }
        }, schemaStore, new Random(1)).generate(schema1, 500);

        if (generated instanceof JSONObject) {
          System.out.println(((JSONObject) generated).toString(2));
        } else if (generated instanceof JSONArray) {
          System.out.println(((JSONArray) generated).toString(2));
        } else {
          System.out.println(generated);
        }
        validate(schema1, generated);

        // Does it also pass Everit?
        if (false)
          if (schema instanceof JSONObject) {
            Schema everitSchema = SchemaLoader.load((JSONObject) schema, url -> {
              url = url.replace("http://localhost:1234", resource.toString());
              try {
                return new URL(url).openStream();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });

            try {
              everitSchema.validate(generated);
            } catch (ValidationException ex) {
              System.out.println(ex.toJSON());
            } catch (Exception e) {
              fail(e);
            }
          }
      }));
    }

    if (false)
      for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
        JSONObject test = tests1.getJSONObject(idx2);
        Object data = test.get("data");
        boolean valid = test.getBoolean("valid");
        String description = test.optString("description", data + (valid ? " succeeds" : " fails"));

        {
          ownTests.add(dynamicTest(description, () -> {
            System.out.println("Schema:");
            if (schema instanceof JSONObject) {
              System.out.println(((JSONObject) schema).toString(2));
            } else {
              System.out.println(schema);
            }
            System.out.println();

            System.out.println("Test:");
            System.out.println(test.toString(2));
            System.out.println();

            List<ValidationError> errors = new ArrayList<>();
            validate(schema1, data, URI.create(""), errors::add);

            if (errors.isEmpty()) {
              // TODO.. add makeFail test.
            }

            System.out.print("Expected to " + (valid ? "pass" : "fail") + " ... ");
            if (errors.isEmpty()) {
              System.out.println("Passed");
            } else {
              System.out.println("Failures:");
              for (ValidationError error : errors) {
                System.out.println(error);
              }
              System.out.println();
            }

            assertEquals(errors.isEmpty(), valid);
          }));
        }
      }

    return dynamicContainer(testSet.getString("description"), ownTests);
  }

  @TestFactory
  DynamicNode own() {
    Path own = FILE_SYSTEM.getPath("/suites").resolve("own");
    return scan(own, own.resolve("remotes"), URI.create("http://json-schema.org/draft-07/schema#"));
  }

  @TestFactory
  DynamicNode draft7Own() {
    Path jsts = FILE_SYSTEM.getPath("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft7"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-07/schema#"));
  }
}
