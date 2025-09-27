plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Harvest 모듈 특화 의존성

    // 카프카 프로듀서
//    implementation("org.springframework.kafka:spring-kafka")
//
//    // XML 파싱
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
//    implementation("javax.xml.bind:jaxb-api:2.3.1")
//
//    // 대용량 파일 처리
//    implementation("org.apache.commons:commons-compress:1.21")
//
//    // 모니터링
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("io.micrometer:micrometer-registry-prometheus")
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}