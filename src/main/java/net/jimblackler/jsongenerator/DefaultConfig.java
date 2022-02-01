package net.jimblackler.jsongenerator;

public class DefaultConfig implements Configuration {

  private boolean pedanticTypes = true;
  private boolean generateNulls = false;
  private boolean generateMinimal = false;
  private boolean generateAdditionalProperties = false;
  private boolean useRomanCharsOnly = true;
  private float nonRequiredPropertyChance = 1.0f;

  public DefaultConfig() {
  }

  public DefaultConfig setPedanticTypes(boolean pedanticTypes) {
    this.pedanticTypes = pedanticTypes;
    return this;
  }

  @Override
  public boolean isPedanticTypes() {
    return pedanticTypes;
  }

  public DefaultConfig setGenerateNulls(boolean generateNulls) {
    this.generateNulls = generateNulls;
    return this;
  }

  @Override
  public boolean isGenerateNulls() {
    return generateNulls;
  }

  public DefaultConfig setGenerateMinimal(boolean generateMinimal) {
    this.generateMinimal = generateMinimal;
    return this;
  }

  @Override
  public boolean isGenerateMinimal() {
    return generateMinimal;
  }

  public DefaultConfig setGenerateAdditionalProperties(boolean generateAdditionalProperties) {
    this.generateAdditionalProperties = generateAdditionalProperties;
    return this;
  }

  @Override
  public boolean isGenerateAdditionalProperties() {
    return generateAdditionalProperties;
  }

  public DefaultConfig setUseRomanCharsOnly(boolean useRomanCharsOnly) {
    this.useRomanCharsOnly = useRomanCharsOnly;
    return this;
  }

  @Override
  public boolean useRomanCharsOnly() {
    return useRomanCharsOnly;
  }

  public DefaultConfig setNonRequiredPropertyChance(float nonRequiredPropertyChance) {
    this.nonRequiredPropertyChance = nonRequiredPropertyChance;
    return this;
  }

  @Override
  public float nonRequiredPropertyChance() {
    return nonRequiredPropertyChance;
  }

}
