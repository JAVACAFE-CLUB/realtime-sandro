plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Common 모듈 의존성
    implementation(project(":common"))

    // Apache Tika - 문서 파싱
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")

    // Bliki - 위키마크업 파싱 (Wikipedia 공식 라이브러리)
    implementation("info.bliki.wiki:bliki-core:3.1.0")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Actuator & Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x:4.11.0")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}