plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Smithy 모듈 특화 의존성
    // 데이터 정제 관련 라이브러리 추가 가능
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}