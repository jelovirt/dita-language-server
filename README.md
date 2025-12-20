# DITA Language Server

dita-language-server (DLS) is an implementation of Language Server Protocol for the DITA markup language.
It provides features like completion and goto definition for many code editors, including VS Code, Emacs and Vim.

## Features

<details>
<summary>Features table</summary>

| DITA Attribute/Element | Diagnostics | Completion | Go to Definition | Hover |
|------------------------|-------------|------------|------------------|-------|
| `keyref`               | ✓           | ✓          | ✓                | ✓     |
| `conkeyref`            | ✓           | ✓          | ✓                | ✓     |
| `href`                 | ✓           |            |                  |       |
| `conref`               | ✓           |            |                  |       |
| Topic `id`             | ✓           |            |                  |       |
| Element `id`           | ✓           |            |                  |       |
| Profiling attributes   | ✓           | ✓          |                  |       |

</details>

### Diagnostics and Validation

* Duplicate topic ID detection
* Duplicate element ID detection within topics
* Keyref and conkeyref validation (missing keys, undefined targets, missing element IDs)
* Cross-reference validation (missing targets, missing topic/element IDs, invalid URIs)
* Profiling attribute value validation against subject scheme

### Code Completion

* Topic and element ID completion for href attributes
* Key completion for keyref and conkeyref attributes
* Element ID completion for keyref/conkeyref with key prefix
* Profiling attribute value completion based on subject scheme

### Go to Definition

* Navigate to key definitions in maps from keyref and conkeyref attributes

### Hover Information

* Display key information (navtitle, target, or text) when hovering over keyref and conkeyref attributes

### Workspace Support

* Set root map via `dita.setRootMap` command

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
