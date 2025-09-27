plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Portal 모듈 특화 의존성
    // API 서빙 관련 라이브러리 추가 가능
    // 다른 모듈과의 의존성 설정 가능
    // implementation(project(":forge"))
    // implementation(project(":scriptorium"))
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}