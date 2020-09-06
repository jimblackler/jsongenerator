package net.jimblacker.jsongenerator;

import com.mifmif.common.regex.Generex;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;

public class PatternReverser {
  public static String reverse(String pattern, int minLength, int maxLength, Random random) {
    if (pattern.startsWith("^")) {
      pattern = pattern.substring("^".length());
    }
    if (pattern.endsWith("$")) {
      pattern = pattern.substring(0, pattern.length() - "$".length());
    }
    Generex generex = new Generex(pattern, random);
    String str = generex.random(minLength, maxLength);

    String urlEncoded = URLEncoder.encode(str);
    return URLDecoder.decode(urlEncoded);
  }
}
