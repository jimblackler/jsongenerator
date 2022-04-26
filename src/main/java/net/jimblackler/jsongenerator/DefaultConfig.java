package net.jimblackler.jsongenerator;

public class DefaultConfig implements Configuration {

  private final boolean pedanticTypes;
  private final boolean generateNulls;
  private final boolean generateMinimal;
  private final boolean generateAdditionalProperties;
  private final boolean useRomanCharsOnly;
  private final float nonRequiredPropertyChance;

  public DefaultConfig(boolean pedanticTypes,
                       boolean generateNulls,
                       boolean generateMinimal,
                       boolean generateAdditionalProperties,
                       boolean useRomanCharsOnly,
                       float nonRequiredPropertyChance) {
    this.pedanticTypes = pedanticTypes;
    this.generateNulls = generateNulls;
    this.generateMinimal = generateMinimal;
    this.generateAdditionalProperties = generateAdditionalProperties;
    this.useRomanCharsOnly = useRomanCharsOnly;
    this.nonRequiredPropertyChance = nonRequiredPropertyChance;
  }

  public static Builder build() {
    return new Builder();
  }

  @Override
  public boolean isPedanticTypes() {
    return pedanticTypes;
  }

  @Override
  public boolean isGenerateNulls() {
    return generateNulls;
  }

  @Override
  public boolean isGenerateMinimal() {
    return generateMinimal;
  }

  @Override
  public boolean isGenerateAdditionalProperties() {
    return generateAdditionalProperties;
  }

  @Override
  public boolean useRomanCharsOnly() {
    return useRomanCharsOnly;
  }

  @Override
  public float nonRequiredPropertyChance() {
    return nonRequiredPropertyChance;
  }

  public static class Builder {

    private boolean pedanticTypes = true;
    private boolean generateNulls = false;
    private boolean generateMinimal = false;
    private boolean generateAdditionalProperties = false;
    private boolean useRomanCharsOnly = true;
    private float nonRequiredPropertyChance = 1.0f;

    private Builder() {
    }

    public Builder setPedanticTypes(boolean pedanticTypes) {
      this.pedanticTypes = pedanticTypes;
      return this;
    }

    public Builder setGenerateNulls(boolean generateNulls) {
      this.generateNulls = generateNulls;
      return this;
    }

    public Builder setGenerateMinimal(boolean generateMinimal) {
      this.generateMinimal = generateMinimal;
      return this;
    }

    public Builder setGenerateAdditionalProperties(boolean generateAdditionalProperties) {
      this.generateAdditionalProperties = generateAdditionalProperties;
      return this;
    }

    public Builder setUseRomanCharsOnly(boolean useRomanCharsOnly) {
      this.useRomanCharsOnly = useRomanCharsOnly;
      return this;
    }

    public Builder setNonRequiredPropertyChance(float nonRequiredPropertyChance) {
      this.nonRequiredPropertyChance = nonRequiredPropertyChance;
      return this;
    }

    public DefaultConfig get() {
      return new DefaultConfig(
          pedanticTypes,
          generateNulls,
          generateMinimal,
          generateAdditionalProperties,
          useRomanCharsOnly,
          nonRequiredPropertyChance);
    }
  }

}
