package net.jimblacker.jsongenerator;

import net.jimblackler.jsonschemafriend.SchemaException;

public class JsonGeneratorException extends SchemaException {
  public JsonGeneratorException() {
    super();
  }

  public JsonGeneratorException(String message) {
    super(message);
  }

  public JsonGeneratorException(String message, Throwable cause) {
    super(message, cause);
  }

  public JsonGeneratorException(Throwable cause) {
    super(cause);
  }

  protected JsonGeneratorException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
