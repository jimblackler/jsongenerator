package net.jimblackler.jsongenerator;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;

public class StringUtils {
  static String randomString(Random random, int minLength, int maxLength) {
    if (maxLength <= minLength) {
      throw new IllegalStateException();
    }
    boolean limitChars = random.nextInt(10) < 8;
    StringBuilder stringBuilder = new StringBuilder();
    int length = random.nextInt(maxLength - minLength) + minLength;
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) (limitChars ? random.nextInt(96) + 32 : random.nextInt()));
    }
    // We don't aim to handle strings that won't survive URL encoding with standard methods.
    String urlEncoded = URLEncoder.encode(stringBuilder.toString());
    return URLDecoder.decode(urlEncoded);
  }
}
