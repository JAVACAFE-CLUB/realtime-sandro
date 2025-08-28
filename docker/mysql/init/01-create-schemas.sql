-- 멀티 모듈 스키마 초기화 스크립트

-- 각 모듈별 스키마 생성
CREATE SCHEMA IF NOT EXISTS harvest_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE SCHEMA IF NOT EXISTS smithy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE SCHEMA IF NOT EXISTS codex_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE SCHEMA IF NOT EXISTS portal_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- realtime 사용자에게 각 스키마별 모든 권한 부여
GRANT ALL PRIVILEGES ON harvest_db.* TO 'realtime'@'%';
GRANT ALL PRIVILEGES ON smithy_db.* TO 'realtime'@'%';
GRANT ALL PRIVILEGES ON codex_db.* TO 'realtime'@'%';
GRANT ALL PRIVILEGES ON portal_db.* TO 'realtime'@'%';

-- 권한 적용
FLUSH PRIVILEGES;

-- 초기화 완료 로그
SELECT 'Multi-schema initialization completed' AS message;