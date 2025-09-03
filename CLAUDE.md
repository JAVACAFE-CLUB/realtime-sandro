# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Kotlin Spring Boot project called "realtime". It contains multiple modules for different functionalities:

- **codex**: 색인 (Indexing module)
- **portal**: 서빙 (Serving module)  
- **harvest**: 수집 (Collection module)
- **smithy**: Smithy module (converted to Python)

## Build System & Commands

This project uses Gradle with the Gradle wrapper:

- **Build the project**: `./gradlew build`
- **Run the application**: `./gradlew bootRun`
- **Run tests**: `./gradlew test`
- **Clean build**: `./gradlew clean build`

## Technology Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.4
- **Java Version**: 21
- **Build Tool**: Gradle (Multi-module)
- **Testing**: JUnit 5, Kotest, Fixture Monkey
- **Dependencies**: Kotlin Coroutines, Jackson Kotlin Module

## Project Structure

```
realtime/                                   # Multi-module root project
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Module declarations
├── codex/                                 # 색인 (Indexing module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/sandro/realtime/codex/
│       └── test/kotlin/
├── portal/                                # 서빙 (Serving module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/sandro/realtime/portal/
│       └── test/kotlin/
├── harvest/                               # 수집 (Collection module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/sandro/realtime/harvest/
│       └── test/kotlin/
├── smithy/                               # Python module (converted from Kotlin)
│   ├── pyproject.toml
│   └── smithy.md
└── src/                                  # Legacy main application (if exists)
    ├── main/kotlin/com/sandro/realtime/
    └── test/kotlin/
```

## Key Dependencies

### Common Dependencies (All Modules)
- `kotlin-reflect` - Kotlin reflection support
- `kotlin-stdlib-jdk8` - Kotlin standard library
- `jackson-module-kotlin` - JSON serialization for Kotlin
- `spring-boot-starter-web` - Web MVC framework  
- `spring-boot-starter-validation` - Validation framework
- `kotlinx-coroutines-core` - Kotlin coroutines support
- `kotlinx-coroutines-reactor` - Reactive coroutines integration

### Testing Dependencies
- `spring-boot-starter-test` - Spring Boot testing
- `kotlin-test-junit5` - Kotlin JUnit 5 integration
- `kotest-runner-junit5` - Kotest test runner
- `kotest-assertions-core` - Kotest assertions
- `kotest-property` - Property-based testing
- `kotest-extensions-spring` - Spring integration for Kotest
- `fixture-monkey-starter-kotlin` - Test fixture generation
- `fixture-monkey-jackson` - JSON fixture support
- `fixture-monkey-jakarta-validation` - Validation fixture support

## Development Notes

- Multi-module Gradle project with shared configuration in root `build.gradle.kts`
- Each module has its own Spring Boot application if needed
- Main package structure: `com.sandro.realtime.[module]`
- Java/Kotlin version: 21
- Uses Kotlin DSL for Gradle configuration
- Testing strategy combines JUnit 5, Kotest, and Fixture Monkey
- Coroutines enabled for reactive programming support