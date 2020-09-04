package net.jimblackler.jsongenerator;

import static net.jimblackler.jsonschemafriend.DocumentUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import net.jimblacker.jsongenerator.Writer;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.json.JSONArray;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class FromInternet {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();
  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Collection<DynamicTest> testsOut = new ArrayList<>();
    Path outDir = FILE_SYSTEM.getPath("out");
    try (
        InputStream inputStream = FromInternet.class.getResourceAsStream("/internetSchemas.json")) {
      JSONArray array = new JSONArray(streamToString(inputStream));
      for (int idx = 0; idx != array.length(); idx++) {
        String str = array.getString(idx);
        testsOut.add(DynamicTest.dynamicTest(str, () -> {
          URI uri = URI.create(str);
          Path out = outDir.resolve(uri.getPath().replace("/", "~").replace(".", "_") + ".json");
          new Writer().build(uri, out, new Random(1));
        }));
      }
    }
    return testsOut;
  }
}
