plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Common 모듈 의존성
    implementation(project(":common"))

    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // 위키 덤프 처리
    implementation("org.apache.commons:commons-compress:1.28.0") // bz2 압축 해제
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.19.2") // Jackson XML 모듈
    implementation("com.fasterxml.woodstox:woodstox-core:6.6.2") // StAX 구현체
    implementation("org.springframework:spring-oxm") // Spring OXM for XML marshalling

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0")

    // Spring Batch Test
    testImplementation("org.springframework.batch:spring-batch-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.bootJar {
    enabled = true
}