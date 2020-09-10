package net.jimblacker.jsongenerator;

public interface Configuration {
  boolean isPedanticTypes();

  boolean isGenerateNulls();

  boolean isGenerateMinimal();

  float nonRequiredPropertyChance();
}
