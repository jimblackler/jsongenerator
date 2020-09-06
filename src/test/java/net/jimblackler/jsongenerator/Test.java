package net.jimblackler.jsongenerator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;
import net.jimblacker.jsongenerator.Writer;
import net.jimblackler.jsonschemafriend.SchemaException;

public class Test {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws URISyntaxException, SchemaException, IOException {
    Path outDir = FILE_SYSTEM.getPath("out");
    Path base = FILE_SYSTEM.getPath("/examples");
    Path file = base.resolve("longread").resolve("schemas").resolve("schema.json");
    Path out = outDir.resolve("example.json");
    new Writer().build(Test.class.getResource(file.toString()).toURI(), out, new Random(1), 250);
  }
}
