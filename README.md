# JSON Generator

A JSON data generator from JSON Schemas, provided as a Java library.

It is available on JitPack.
[![](https://jitpack.io/v/net.jimblackler/jsongenerator.svg)](https://jitpack.io/#net.jimblackler/jsongenerator)

An online demonstration [is here](https://tryjsonschematypes.appspot.com/#generate).

## How to Use

```java
import java.util.Random;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class JsonGenerationExample {

  /**
   * Generate random Json with the following schema:
   * {
   *   "type": "object",
   *   "properties": {
   *     "name": { "type": "string" },
   *     "birthday": { "type": "string","format": "date" },
   *     "age": { "type": "integer" }
   *   }
   * }
   */
  public static void main(String[] args) throws Exception {
    Configuration config = DefaultConfig.build()
        .setGenerateMinimal(false)
        .setNonRequiredPropertyChance(0.5f)
        .get();
    SchemaStore schemaStore = new SchemaStore(true);
    Schema schema = schemaStore.loadSchemaJson("{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" }, \"birthday\": { \"type\": \"string\", \"format\": \"date\" }, \"age\": { \"type\": \"integer\" } } }");
    Generator generator = new Generator(config, schemaStore, new Random());
    Object json = generator.generate(schema, 10);

    // sample output: {name=vxtydd, birthday=2377-03-08, age=544}
    System.out.println(json);
  }

}
```

## License
Written by jimblackler@gmail.com and offered under an Apache 2.0 license.
