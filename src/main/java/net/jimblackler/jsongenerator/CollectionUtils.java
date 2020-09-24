package net.jimblackler.jsongenerator;

import java.util.Collection;
import java.util.Random;

public class CollectionUtils {
  public static <T> T randomElement(Random random, Collection<T> collection) {
    int size = collection.size();
    if (size == 0) {
      throw new IllegalStateException();
    }
    int selection = random.nextInt(size);
    for (T element : collection) {
      if (selection == 0) {
        return element;
      }
      selection--;
    }
    throw new IllegalStateException();
  }
}
