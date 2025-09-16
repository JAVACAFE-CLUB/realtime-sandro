plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}