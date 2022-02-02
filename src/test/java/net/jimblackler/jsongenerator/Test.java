package net.jimblackler.jsongenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Test {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws URISyntaxException, SchemaException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    Path outDir = FILE_SYSTEM.getPath("out");
    if (!outDir.toFile().exists()) {
      outDir.toFile().mkdir();
    }
    Path base = FILE_SYSTEM.getPath("/examples");
    Path file = base.resolve("warnings.schema.json");
    Path out = outDir.resolve("example.json");
    SchemaStore schemaStore = new SchemaStore();
    Schema schema = schemaStore.loadSchema(Test.class.getResource(file.toString()).toURI());

    Configuration config = DefaultConfig.build()
        .setPedanticTypes(false)
        .setGenerateNulls(false)
        .setGenerateMinimal(true)
        .setGenerateAdditionalProperties(false)
        .setUseRomanCharsOnly(false)
        .setNonRequiredPropertyChance(0.5f)
        .get();
    Object object = new Generator(config, schemaStore, new Random(1)).generate(schema, 16);

    new Validator().validate(schema, object);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(out.toFile()))) {
      writer.write(objectWriter.writeValueAsString(object));
    }
  }
}
