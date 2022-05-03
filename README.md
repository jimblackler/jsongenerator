# JSON Generator

A JSON data generator from JSON Schemas, provided as a Java library.

This is a Json object generator used in Airbyte. It is based on the [jsongenerator](https://github.com/jimblackler/jsongenerator) authored by [Jim Blackler](https://github.com/jimblackler).

## Changes
- Add a `DefaultConfig` implementation of the `Configuration` interface.
- Generate valid `date` and `date-time` fields more efficiently.
- Set min object properties to `1`.
- Set min array items to `1`.

## License
The original [jsongenerator](https://github.com/jimblackler/jsongenerator) is authored by  is licensed under [Apache 2.0](LICENSE).
