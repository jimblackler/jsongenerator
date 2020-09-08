package net.jimblacker.jsongenerator;

import static net.jimblacker.jsongenerator.CacheLoader.load;

import java.io.IOException;
import java.net.URI;
import java.util.Random;
import javax.script.ScriptException;

public class PatternReverser {
  private final javax.script.ScriptEngine scriptEngine =
      new javax.script.ScriptEngineManager().getEngineByName("js");

  public PatternReverser() throws IOException {
    String s = load(URI.create(
        "https://raw.githubusercontent.com/fent/randexp.js/master/build/randexp.min.js"));
    try {
      scriptEngine.eval("window = {};");
      scriptEngine.eval(s);
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
  }

  public String reverse(String pattern, int minLength, int maxLength, Random random) {
    // TODO: use random seed, at least.
    scriptEngine.put("pattern", pattern);

    try {
      Object eval = scriptEngine.eval("new window.RandExp(pattern).gen();");
      return eval.toString();
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
  }
}
