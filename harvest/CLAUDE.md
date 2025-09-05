# CLAUDE.md - Harvest 모듈

이 파일은 Claude Code가 harvest 모듈 작업 시 참고하는 가이드입니다.

## 프로젝트 개요

Harvest는 위키피디아 덤프 파일을 MinIO 객체 스토리지에 업로드하는 데이터 수집 모듈입니다. FastAPI 기반의 웹 서버로 구성되어 있어 REST API를 통해 데이터 수집 작업을 수행할 수 있습니다.

## 기술 스택

- **언어**: Python 3.12+
- **웹 프레임워크**: FastAPI 
- **패키지 관리**: uv (Python 패키지 매니저)
- **주요 의존성**: boto3, confluent-kafka, newspaper3k, python-dotenv, uvicorn
- **스토리지**: MinIO (S3 호환)
- **컨테이너**: Docker Compose

## 개발 규칙

### Python 명령어 사용 규칙

**중요: 모든 Python 명령어는 `uv`를 사용합니다.**

```bash
# ❌ 사용하지 않음
python script.py
pip install package

# ✅ 올바른 사용법
uv run python script.py
uv add package
uv sync
```

### 주요 명령어

**환경 설정**:

```bash
# 가상환경 및 의존성 설치
uv sync

# 의존성 추가
uv add package_name

# 개발용 의존성 추가
uv add --dev package_name
```

**웹 서버 실행**:

```bash
# 개발용 서버 시작 (자동 리로드)
uv run python run_server.py

# 또는 직접 uvicorn 실행
uv run uvicorn src.harvest.api.main:app --host 0.0.0.0 --port 8000 --reload

# API 문서 확인
# http://localhost:8000/docs (Swagger UI)
# http://localhost:8000/redoc (ReDoc)
```

**테스트 및 검증**:

```bash
# 환경변수 설정 테스트
uv run python tests/test_env_config.py

# MinIO 연결 테스트 (Docker 서비스 필요)
uv run python tests/test_minio_connection.py

# 테스트 실행 (pytest가 설치된 경우)
uv run pytest

# 코드 포맷팅 (black이 설치된 경우)
uv run black .

# 린팅 (ruff가 설치된 경우)
uv run ruff check .
```

**레거시 스크립트 실행**:

```bash
# 기존 CLI 방식 위키 데이터 업로드 (직접 실행)
uv run python upload_wiki_data.py
```

## 프로젝트 구조

```
harvest/
├── src/
│   └── harvest/
│       ├── __init__.py
│       ├── api/                           # FastAPI 웹 API
│       │   ├── __init__.py
│       │   ├── main.py                    # FastAPI 앱 진입점
│       │   └── routers/                   # API 라우터들
│       │       ├── __init__.py
│       │       ├── health.py              # 헬스체크 API
│       │       └── wiki.py                # 위키 데이터 API
│       ├── services/                      # 비즈니스 로직 서비스들
│       │   ├── __init__.py
│       │   ├── minio_service.py           # MinIO 작업 서비스
│       │   ├── upload_service.py          # 파일 업로드 서비스
│       │   └── wiki_service.py            # 위키 데이터 서비스
│       ├── core/                          # 핵심 설정 및 유틸리티
│       │   ├── __init__.py
│       │   └── config.py                  # 설정 관리
│       └── schemas/                       # Pydantic 데이터 모델
│           ├── __init__.py
│           └── wiki.py                    # 위키 API 스키마
├── tests/
│   ├── test_minio_connection.py           # MinIO 연결 테스트
│   └── test_env_config.py                 # 환경 설정 테스트
├── run_server.py                          # 서버 시작 스크립트
├── upload_wiki_data.py                    # 레거시 CLI 스크립트
├── pyproject.toml                         # 프로젝트 설정
├── uv.lock                                # 의존성 락 파일
├── .env.example                           # 환경변수 템플릿
└── CLAUDE.md                              # 이 파일
```

## API 엔드포인트

### 헬스체크
- `GET /health` - 기본 헬스체크
- `GET /health/status` - 서비스 상태 (MinIO 연결 포함)

### 위키 데이터 관리
- `GET /wiki/files` - 업로드 가능한 파일 목록 조회
- `POST /wiki/upload/all` - 모든 파일 업로드
- `POST /wiki/upload/{filename}` - 특정 파일 업로드
- `GET /wiki/buckets` - MinIO 버킷 목록 조회
- `POST /wiki/buckets/{bucket_name}` - 새 버킷 생성

## 환경 설정

### .env 파일 설정

`.env.example`을 참고하여 `.env` 파일을 생성하고 다음 설정을 확인하세요:

```bash
# 위키 데이터 업로드 설정
WIKI_DATA_DIR=/path/to/your/bigdata
WIKI_BUCKET_NAME=wiki-data

# MinIO 연결 설정
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minio
MINIO_SECRET_KEY=minio123
MINIO_REGION=us-east-1

# 로그 설정
LOG_FILE_PATH=upload.log
LOG_LEVEL=INFO

# 업로드 성능 설정
MULTIPART_THRESHOLD=26214400  # 25MB
MAX_CONCURRENCY=10

# 재시도 설정
MAX_RETRIES=3
RETRY_DELAY=5
```

### MinIO 서비스 시작

```bash
# 프로젝트 루트에서 실행
docker-compose up -d minio

# MinIO 웹 콘솔: http://localhost:9001
# 사용자명: minio, 비밀번호: minio123
```

## 아키텍처 설계

### 레이어드 아키텍처
- **API Layer**: FastAPI 라우터, 요청/응답 처리
- **Service Layer**: 비즈니스 로직, 도메인 서비스
- **Core Layer**: 설정 관리, 공통 유틸리티

### 주요 서비스 클래스
- **WikiService**: 위키 데이터 관련 비즈니스 로직
- **UploadService**: 파일 업로드 로직 (재시도, 진행률 등)
- **MinIOService**: MinIO 객체 스토리지 작업

### 데이터 검증
- **Pydantic**: API 요청/응답 스키마 정의 및 검증
- **Settings**: 환경변수 자동 로드 및 타입 검증

## 코딩 스타일

- **PEP 8** 준수
- **타입 힌트** 사용 권장
- **Docstring** 작성 (Google 스타일)
- **환경변수 활용**: 하드코딩 대신 환경변수 사용
- **로깅 활용**: print 대신 logging 모듈 사용
- **레이어 분리**: API → Service → Core 계층 구조 유지
- **의존성 주입**: 서비스 간 의존성을 생성자를 통해 주입

## 작업 완료 후 확인사항

1. **환경변수 설정 확인**: 모든 필요한 환경변수가 .env에 설정되어 있는지 확인
2. **서비스 상태 확인**: `uv run python tests/test_env_config.py`로 서비스 초기화 확인
3. **MinIO 연결 테스트**: `uv run python tests/test_minio_connection.py` (Docker 서비스 필요)
4. **의존성 동기화**: `uv sync`로 의존성이 최신 상태인지 확인
5. **웹 서버 실행**: `uv run python run_server.py`로 서버 정상 시작 확인
6. **API 문서 확인**: http://localhost:8000/docs에서 API 문서 확인
7. **로그 파일 확인**: upload.log에서 오류가 없는지 확인

## 문제 해결

### 일반적인 문제들

1. **MinIO 연결 실패**:
   ```bash
   # MinIO 컨테이너 상태 확인
   docker ps | grep minio
   
   # MinIO 서비스 재시작
   docker-compose restart minio
   ```

2. **Python 패키지 문제**:
   ```bash
   # 의존성 재설치
   uv sync --reinstall
   
   # 캐시 정리
   uv cache clean
   ```

3. **환경변수 로드 실패**:
    - `.env` 파일이 프로젝트 루트에 있는지 확인
    - 파일 권한 확인
    - 환경변수명 오타 확인

4. **Import 경로 오류**:
    - 새로운 구조에서는 `from harvest.services.* import *` 패턴 사용
    - 테스트 파일의 경로 설정이 올바른지 확인 (`project_root = Path(__file__).parent.parent`)

## 레거시 지원

- **upload_wiki_data.py**: 기존 CLI 방식 스크립트는 유지되며 계속 사용 가능
- **이전 import 경로**: 새로운 서비스 구조로 완전히 마이그레이션됨
- **설정 파일**: settings.py → core/config.py로 이동

## API 사용 예시

```bash
# 헬스체크
curl http://localhost:8000/health

# 파일 목록 조회
curl http://localhost:8000/wiki/files

# 모든 파일 업로드 (백그라운드 작업)
curl -X POST http://localhost:8000/wiki/upload/all

# 특정 파일 업로드
curl -X POST http://localhost:8000/wiki/upload/enwiki-20240101-pages-articles.xml.bz2
```

## 참고사항

- **uv 사용 이유**: 빠른 패키지 설치, 의존성 해결, 가상환경 관리가 통합된 최신 Python 패키지 매니저
- **FastAPI 선택 이유**: 빠른 성능, 자동 API 문서 생성, 타입 안전성, 비동기 지원
- **환경변수 우선순위**: 코드에서 전달된 매개변수 > 환경변수 > 기본값
- **로그 레벨**: DEBUG < INFO < WARNING < ERROR < CRITICAL
- **서비스 레이어**: 비즈니스 로직을 API에서 분리하여 재사용성과 테스트 용이성 향상