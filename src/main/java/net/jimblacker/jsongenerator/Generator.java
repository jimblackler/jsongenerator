package net.jimblacker.jsongenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class Generator {
  private final URI defaultMetaSchema = URI.create("http://json-schema.org/draft-07/schema#");

  public void build(URI uri, Path out) throws GenerationException, IOException {
    SchemaStore schemaStore = new SchemaStore();
    Schema schema = schemaStore.loadSchema(uri, defaultMetaSchema);

    String str = "test";
    try (BufferedWriter writer =
             new BufferedWriter(new FileWriter(out.toFile(), StandardCharsets.UTF_8))) {
      writer.write(str);
    }
  }
}
