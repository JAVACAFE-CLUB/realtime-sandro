# Harvest - 위키 데이터 수집 모듈

이 모듈은 위키피디아 덤프 파일을 MinIO 객체 스토리지에 업로드하는 기능을 제공합니다.

## 기능

- boto3를 사용한 MinIO S3 호환 API 연동
- 대용량 파일 멀티파트 업로드 지원
- 업로드 진행률 실시간 표시
- 자동 재시도 및 오류 처리
- 파일 무결성 검증
- 중복 업로드 방지

## 설치 및 설정

### 1. 의존성 설치

```bash
pip install boto3 confluent-kafka newspaper3k
```

또는 프로젝트 루트에서:

```bash
pip install -e .
```

### 2. MinIO 서비스 시작

프로젝트 루트에서 docker-compose를 사용하여 MinIO 서비스를 시작합니다:

```bash
docker-compose up -d minio
```

MinIO 웹 콘솔 접속:

- URL: http://localhost:9001
- 사용자명: minio
- 비밀번호: minio123

## 사용 방법

### 1. MinIO 연결 테스트

```bash
cd /Users/sandeulpark/personal/realtime/harvest
python test_minio_connection.py
```

### 2. 위키 데이터 업로드

```bash
cd /Users/sandeulpark/personal/realtime/harvest
python upload_wiki_data.py
```

## 프로젝트 구조

```
harvest/
├── src/
│   └── harvest/
│       ├── __init__.py
│       ├── minio_uploader.py      # MinIO 업로더 클래스
│       └── upload_wiki_data.py    # 메인 업로드 로직
├── pyproject.toml                 # 프로젝트 설정
├── upload_wiki_data.py           # 실행 스크립트
├── test_minio_connection.py      # 연결 테스트 스크립트
└── README.md                     # 이 파일
```

## 설정

### 기본 설정

- **데이터 디렉토리**: `/Users/sandeulpark/bigdata`
- **MinIO 엔드포인트**: `http://localhost:9000`
- **버킷명**: `wiki-data`
- **업로드 경로**: `raw/` 접두사 사용

### 사용자 정의 설정

MinIO 연결 설정을 변경하려면 `MinIOUploader` 클래스 초기화 시 매개변수를 수정하세요:

```python
uploader = MinIOUploader(
    endpoint_url="http://your-minio-server:9000",
    aws_access_key_id="your-access-key",
    aws_secret_access_key="your-secret-key"
)
```

## 특징

### 1. 멀티파트 업로드

- 25MB 이상의 파일은 자동으로 멀티파트 업로드 사용
- 업로드 중단 시 재시작 가능
- 병렬 청크 업로드로 성능 향상

### 2. 진행률 표시

실시간으로 업로드 진행률을 확인할 수 있습니다:

```
업로드 진행률 [kowiki-20250820-pages-articles-multistream1.xml-p1p82407.bz2]: 45.2% (123,456,789/273,123,456 bytes)
```

### 3. 메타데이터 추가

업로드되는 파일에는 다음 메타데이터가 자동으로 추가됩니다:

- `source`: 데이터 소스 (bigdata)
- `upload-date`: 업로드 날짜
- `original-name`: 원본 파일명
- `file-type`: 파일 타입 (wiki-dump)

### 4. 오류 처리

- 네트워크 오류 시 자동 재시도 (최대 3회)
- 파일 크기 검증으로 무결성 확인
- 상세한 로그 기록 (upload.log 파일)

## 로그

업로드 과정은 다음 위치에 로그로 기록됩니다:

- 콘솔 출력: 실시간 진행 상황
- 파일 로그: `upload.log` (harvest 디렉토리 내)

## 문제 해결

### MinIO 연결 실패

```bash
# MinIO 컨테이너 상태 확인
docker ps | grep minio

# MinIO 서비스 재시작
docker-compose restart minio

# 로그 확인
docker logs realtime-minio
```

### 업로드 실패

1. 네트워크 연결 확인
2. 디스크 공간 확인
3. MinIO 서버 상태 확인
4. `upload.log` 파일에서 상세 오류 메시지 확인

## 예제

### 단일 파일 업로드

```python
from src.harvest.minio_uploader import MinIOUploader

uploader = MinIOUploader()

# 파일 업로드
success = uploader.upload_file(
    "/path/to/file.bz2",
    "wiki-data",
    "raw/file.bz2"
)

if success:
    print("업로드 성공!")
else:
    print("업로드 실패!")
```

### 진행률 콜백 사용

```python
def progress_callback(progress):
    print(f"진행률: {progress.progress_percent:.1f}%")

uploader.upload_file(
    "/path/to/file.bz2",
    "wiki-data",
    "raw/file.bz2",
    progress_callback=progress_callback
)
```