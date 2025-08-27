plugins {
    // 플러그인은 루트 프로젝트에서 상속받음
}

dependencies {
    // Codex 모듈 특화 의존성
    // 검색 및 인덱싱 관련 라이브러리 추가 가능
    // 예: Elasticsearch, Lucene 등
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = true
}