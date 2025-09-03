# CLAUDE.md - Harvest 모듈

이 파일은 Claude Code가 harvest 모듈 작업 시 참고하는 가이드입니다.

## 프로젝트 개요

Harvest는 위키피디아 덤프 파일을 MinIO 객체 스토리지에 업로드하는 데이터 수집 모듈입니다.

## 기술 스택

- **언어**: Python 3.12+
- **패키지 관리**: uv (Python 패키지 매니저)
- **주요 의존성**: boto3, confluent-kafka, newspaper3k, python-dotenv
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

**실행 명령어**:

```bash
# MinIO 연결 테스트
uv run python tests/test_minio_connection.py

# 위키 데이터 업로드
uv run python upload_wiki_data.py

# 특정 모듈 실행
uv run python -m harvest.upload_wiki_data
```

**테스트 및 개발**:

```bash
# 테스트 실행 (pytest가 설치된 경우)
uv run pytest

# 코드 포맷팅 (black이 설치된 경우)
uv run black .

# 린팅 (ruff가 설치된 경우)
uv run ruff check .
```

## 프로젝트 구조

```
harvest/
├── src/
│   └── harvest/
│       ├── __init__.py
│       ├── minio_uploader.py      # MinIO 업로더 클래스
│       └── upload_wiki_data.py    # 메인 업로드 로직
├── tests/
│   ├── test_minio_connection.py   # 연결 테스트
│   └── test_env_config.py         # 환경 설정 테스트
├── upload_wiki_data.py            # 실행 스크립트
├── pyproject.toml                 # 프로젝트 설정
├── uv.lock                        # 의존성 락 파일
├── .env.example                   # 환경변수 템플릿
└── CLAUDE.md                      # 이 파일
```

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

## 코딩 스타일

- **PEP 8** 준수
- **타입 힌트** 사용 권장
- **Docstring** 작성 (Google 스타일)
- **환경변수 활용**: 하드코딩 대신 환경변수 사용
- **로깅 활용**: print 대신 logging 모듈 사용

## 작업 완료 후 확인사항

1. **환경변수 설정 확인**: 모든 필요한 환경변수가 .env에 설정되어 있는지 확인
2. **MinIO 연결 테스트**: `uv run python tests/test_minio_connection.py`
3. **의존성 동기화**: `uv sync`로 의존성이 최신 상태인지 확인
4. **로그 파일 확인**: upload.log에서 오류가 없는지 확인

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

## 참고사항

- **uv 사용 이유**: 빠른 패키지 설치, 의존성 해결, 가상환경 관리가 통합된 최신 Python 패키지 매니저
- **환경변수 우선순위**: 코드에서 전달된 매개변수 > 환경변수 > 기본값
- **로그 레벨**: DEBUG < INFO < WARNING < ERROR < CRITICAL