package net.jimblacker.jsongenerator;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;

public class StringUtils {
  static String randomString(Random random, int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) random.nextInt());
    }
    // We don't aim to handle strings that won't survive URL encoding with standard methods.
    String urlEncoded = URLEncoder.encode(stringBuilder.toString());
    return URLDecoder.decode(urlEncoded);
  }
}
