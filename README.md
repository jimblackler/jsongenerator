# JSON Generator

[![Java CI](https://github.com/airbytehq/jsongenerator/actions/workflows/java_ci.yaml/badge.svg)](https://github.com/airbytehq/jsongenerator/actions/workflows/java_ci.yaml) [![JitPack](https://jitpack.io/v/airbytehq/jsongenerator.svg)](https://jitpack.io/#airbytehq/jsongenerator)

This is a Json object generator used in Airbyte. It is based on the [jsongenerator](https://github.com/jimblackler/jsongenerator) authored by [Jim Blackler](https://github.com/jimblackler).

## Install
This library is available on JitPack. Check [here](https://jitpack.io/#airbytehq/jsongenerator) for details.

1. Add the JitPack repository to the root `build.gradle`:
  ```groovy
  allprojects {
    repositories {
      maven { url 'https://jitpack.io' }
    }
  }
  ```
2. Add the dependency
  ```groovy
  dependencies {
    implementation 'com.github.airbytehq:jsongenerator:<version>'
  }
  ```

## Publish
To publish a new version, tag the desired commit with a new version label.

```bash
VERSION=<new-version>
git tag -a "${VERSION}" -m "Version ${VERSION}"
git push origin "${VERSION}"
```

## Changes
- Add a `DefaultConfig` implementation of the `Configuration` interface.
- Generate valid `date` and `date-time` fields more efficiently.
- Set min object properties to `1`.
- Set min array items to `1`.

## License
The original [jsongenerator](https://github.com/jimblackler/jsongenerator) is licensed under [Apache 2.0](LICENSE).

## CHANGELOG

| Version | Description |
| ------- | ----------- |
| 1.0.0   | Publish to JitPack. |
