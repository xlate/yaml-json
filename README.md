# yaml-json : JSON API for YAML
![build](https://github.com/xlate/yaml-json/workflows/build/badge.svg?branch=main) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xlate_yaml-json&metric=alert_status)](https://sonarcloud.io/dashboard?id=xlate_yaml-json) [![Maven Central](https://img.shields.io/maven-central/v/io.xlate/yaml-json)](https://search.maven.org/artifact/io.xlate/yaml-json) [![javadoc](https://javadoc.io/badge2/io.xlate/yaml-json/javadoc.svg)](https://javadoc.io/doc/io.xlate/yaml-json)

Read and write YAML in Java using the Jakarta JSON API

# Basic Usage

```java
Reader reader = ...;

try (JsonParser parser = Yaml.createParser(reader)) {
    // Read YAML via the JsonParser
}
```

# Versions
This library is built for multiple combinations of JSON API and Yaml versions.

## Supported JSON API dependencies (pick one):
1. Jakarta JSON (module `jakarta.json`):
  ```xml
  <dependency>
    <groupId>io.xlate</groupId>
    <artifactId>yaml-json</artifactId>
    <version>${version.yaml-json.current}</version>
  </dependency>
  ```
1. Legacy Java EE JSON (module `java.json`):
  ```xml
  <dependency>
    <groupId>io.xlate</groupId>
    <artifactId>yaml-json</artifactId>
    <version>${version.yaml-json.current}</version>
    <classifier>legacy</classifier>
  </dependency>
  ```

## Supported YAML Versions
Placing one of the two support YAML libraries on the class/module path will enable
that library within `yaml-json`. Note, when using JPMS modules, you must also add
the module via the `java` command.

1. SnakeYaml (YAML 1.1):
  ```xml
  <dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>${version.snakeyaml}</version>
  </dependency>
  ```
  Add to `java` command: `--add-modules=org.yaml.snakeyaml`

1. SnakeYaml Engine (YAML 1.2):
  ```xml
  <dependency>
    <groupId>org.snakeyaml</groupId>
    <artifactId>snakeyaml-engine</artifactId>
    <version>${version.snakeyaml-engine}</version>
  </dependency>
  ```
  Add to `java` command: `--add-modules=org.snakeyaml.engine.v2`

