# DITA Language Server

## Development

Prerequisites:

* Java 17 or newer
* Gradle

Build code

```shell
./gradlew compileJava
```

Run tests

```shell
./gradlew test
```

Format code

```shell
./gradlew spotlessApply
```

## Distribution

Build distribution JAR file

```shell
./gradlew clean shadowJar
```
