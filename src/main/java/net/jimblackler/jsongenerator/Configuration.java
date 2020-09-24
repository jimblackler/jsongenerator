package net.jimblackler.jsongenerator;

public interface Configuration {
  boolean isPedanticTypes();

  boolean isGenerateNulls();

  boolean isGenerateMinimal();

  float nonRequiredPropertyChance();
}
