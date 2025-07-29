# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application written in Kotlin called "realtime-sandro". It's a minimal Spring Boot web application with basic configuration.

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
- **Build Tool**: Gradle
- **Testing**: JUnit 5

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/sandro/realtime/
│   │   └── RealtimeSandroApplication.kt    # Main application class
│   └── resources/
│       ├── application.yml                 # Spring configuration
│       ├── static/                        # Static web assets
│       └── templates/                     # View templates
└── test/
    └── kotlin/com/sandro/realtime/
        └── RealtimeSandroApplicationTests.kt # Basic Spring Boot test
```

## Key Dependencies

- `spring-boot-starter-web` - Web MVC framework
- `jackson-module-kotlin` - JSON serialization for Kotlin
- `kotlin-reflect` - Kotlin reflection support
- `spring-boot-starter-test` - Testing framework

## Development Notes

- The application uses Spring Boot's auto-configuration
- Main package: `com.sandro.realtime`
- Application name configured as "realtime-sandro" in application.yml
- Basic context loading test is included