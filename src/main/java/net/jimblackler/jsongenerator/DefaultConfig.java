package net.jimblackler.jsongenerator;

public class DefaultConfig implements Configuration {

  @Override
  public boolean isPedanticTypes() {
    return true;
  }

  @Override
  public boolean isGenerateNulls() {
    return false;
  }

  @Override
  public boolean isGenerateMinimal() {
    return false;
  }

  @Override
  public boolean isGenerateAdditionalProperties() {
    return false;
  }

  @Override
  public boolean useRomanCharsOnly() {
    return true;
  }

  @Override
  public float nonRequiredPropertyChance() {
    return 1.0f;
  }

}
