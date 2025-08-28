import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.5.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.sandro"
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = "com.sandro.realtime"
    version = "0.0.1-SNAPSHOT"

    dependencies {
        val implementation by configurations
        val testImplementation by configurations
        val testRuntimeOnly by configurations
        val runtimeOnly by configurations

        // Kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        // Spring Boot
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-validation")

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

        // API Documentation
//        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

        // Testing
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
//        testImplementation("org.testcontainers:junit-jupiter")
//        testImplementation("org.testcontainers:mysql")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
        testImplementation("io.kotest:kotest-assertions-core:5.8.0")
        testImplementation("io.kotest:kotest-property:5.8.0")
        testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
        testImplementation("com.navercorp.fixturemonkey:fixture-monkey-starter-kotlin:1.1.14")
        testImplementation("com.navercorp.fixturemonkey:fixture-monkey-jackson:1.1.14")
        testImplementation("com.navercorp.fixturemonkey:fixture-monkey-jakarta-validation:1.1.14")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}