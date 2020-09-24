package net.jimblackler.jsongenerator;

import static net.jimblackler.jsonschemafriend.CacheLoader.load;
import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.net.URI;
import java.util.Random;
import javax.script.ScriptException;

public class PatternReverser {
  private final javax.script.ScriptEngine scriptEngine =
      new javax.script.ScriptEngineManager().getEngineByName("js");

  public PatternReverser() throws IOException {
    String randexp_js = load(URI.create(
        "https://raw.githubusercontent.com/fent/randexp.js/master/build/randexp.min.js"));
    String random_js = streamToString(PatternReverser.class.getResourceAsStream("/random.js"));
    try {
      scriptEngine.eval("window = {};");
      scriptEngine.eval(randexp_js);
      scriptEngine.eval(random_js);
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
  }

  public String reverse(String pattern, Random random) {
    scriptEngine.put("pattern", pattern);
    scriptEngine.put("seed", random.nextInt());

    try {
      scriptEngine.eval("window.seed = seed;");
      Object eval = scriptEngine.eval("new window.RandExp(pattern).gen();");
      return eval.toString();
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
  }
}
