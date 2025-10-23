# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Kotlin Spring Boot project called "realtime". It contains multiple modules for different functionalities:

- **common**: 공통 (Common shared module)
- **harvest**: 수집 (Data collection module - news, wiki)
- **smithy**: 정제 (Data refinement module - mixed Kotlin/Python)
- **codex**: 색인 (Indexing module)
- **portal**: 서빙 (Serving module)

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
├── docker-compose.yml                     # Docker container orchestration
├── common/                                # 공통 (Common shared module)
│   ├── build.gradle.kts
│   └── src/
│       └── main/kotlin/com/sandro/realtime/common/
├── harvest/                               # 수집 (Data collection module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/sandro/realtime/harvest/
│       │   ├── HarvestApplication.kt      # Main application
│       │   ├── news/                      # News collection
│       │   ├── wiki/                      # Wikipedia collection
│       │   └── common/                    # Shared harvest utilities
│       └── test/kotlin/
├── smithy/                                # 정제 (Data refinement module)
│   ├── build.gradle.kts                   # Kotlin build config
│   └── src/
│       ├── main/kotlin/com/sandro/realtime/smithy/
│       └── [Python modules for data processing]
├── codex/                                 # 색인 (Indexing module)
│   ├── build.gradle.kts
│   └── src/
│       └── main/kotlin/com/sandro/realtime/codex/
└── portal/                                # 서빙 (Serving module)
    ├── build.gradle.kts
    └── src/
        └── main/kotlin/com/sandro/realtime/portal/
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

### Module-Specific Dependencies

#### Harvest Module (Data Collection)
- `spring-boot-starter-batch` - Spring Batch for data processing
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-kafka` - Kafka messaging
- `spring-boot-starter-webflux` - WebClient for HTTP calls
- `jsoup` - HTML parsing
- `spring-boot-starter-data-jpa` - JPA for relational database
- `mysql-connector-j` - MySQL database driver
- `commons-compress` - Compression utilities (bz2 files)
- `jackson-dataformat-xml` - XML processing
- `woodstox-core` - StAX XML implementation

#### Common Module
- `KafkaTopic` - Shared Kafka topic definitions

#### Smithy Module (Data Refinement)
- Mixed Kotlin/Python implementation
- Python modules for data processing
- Kotlin Spring Boot base structure

#### Codex & Portal Modules
- Basic Spring Boot setup with common dependencies

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
- `mockito-kotlin` & `mockk` - Mocking frameworks
- `spring-batch-test` - Spring Batch testing
- `spring-kafka-test` - Kafka testing
- `de.flapdoodle.embed.mongo.spring30x` - Embedded MongoDB for testing

## Development Notes

- Multi-module Gradle project with shared configuration in root `build.gradle.kts`
- Each module has its own Spring Boot application
- Main package structure: `com.sandro.realtime.[module]`
- Java/Kotlin version: 21
- Uses Kotlin DSL for Gradle configuration
- Testing strategy combines JUnit 5, Kotest, and Fixture Monkey
- Coroutines enabled for reactive programming support

## Module Details

### Harvest Module (수집)
- **Purpose**: Data collection from various sources (news, Wikipedia)
- **Main Components**:
  - News collection service with article extraction
  - Wikipedia dump processing with batch jobs
  - Kafka integration for data streaming
  - MongoDB and MySQL data storage
- **Key Features**:
  - HTML parsing and content extraction
  - Compressed file processing (bz2)
  - Scheduled data collection
  - Event-driven architecture

### Smithy Module (정제)  
- **Purpose**: Data refinement and processing
- **Implementation**: Mixed Kotlin/Python architecture
- **Key Features**:
  - Python-based data processing pipelines
  - Kafka message handling
  - Data transformation and enrichment

### Common Module (공통)
- **Purpose**: Shared utilities and configurations
- **Contents**: Kafka topic definitions and common utilities

### Codex Module (색인)
- **Purpose**: Data indexing and search capabilities
- **Status**: Basic Spring Boot structure

### Portal Module (서빙)
- **Purpose**: API serving and web interface
- **Status**: Basic Spring Boot structure

## Docker Support

The project includes `docker-compose.yml` for container orchestration, supporting the multi-service architecture with databases and message queues.