plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Swagger/OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Testing (추가)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}