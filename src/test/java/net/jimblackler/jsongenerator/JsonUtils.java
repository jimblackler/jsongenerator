package net.jimblackler.jsongenerator;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtils {
  public static String toString(Object object) {
    if (object == null) {
      return "null";
    }
    if (object instanceof JSONObject) {
      return ((JSONObject) object).toString(2);
    }
    if (object instanceof JSONArray) {
      return ((JSONArray) object).toString(2);
    }
    return object.toString();
  }
}
