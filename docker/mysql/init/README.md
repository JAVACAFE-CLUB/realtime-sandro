# MySQL 초기화 스크립트

MySQL 컨테이너 시작 시 자동으로 실행되는 초기화 스크립트들입니다.

## 실행 순서

1. `01-create-schemas.sql` - 모듈별 스키마 생성

## 스키마 구성

| 모듈      | 스키마명       | 용도                          |
|---------|------------|-----------------------------|
| harvest | harvest_db | 데이터 수집 및 Spring Batch 메타데이터 |
| smithy  | smithy_db  | 텍스트 처리 및 형태소 분석 결과          |
| codex   | codex_db   | 색인 및 검색 데이터                 |
| portal  | portal_db  | API 서비스 관련 데이터              |

## 사용자 권한

- 사용자: `realtime`
- 패스워드: `realtime1234`
- 권한: 각 스키마에 대한 모든 권한 (CREATE, SELECT, INSERT, UPDATE, DELETE, etc.)

## 확인 방법

```sql
-- 스키마 목록 확인
SHOW DATABASES;

-- 권한 확인
SHOW GRANTS FOR 'realtime'@'%';
```