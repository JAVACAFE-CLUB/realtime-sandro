"""Pydantic 기반 설정 관리"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """애플리케이션 설정 클래스"""

    # MinIO 연결 설정
    minio_endpoint: str = Field(
        default="http://localhost:9000",
        description="MinIO 서버 엔드포인트"
    )
    minio_access_key: str = Field(
        description="MinIO 액세스 키"
    )
    minio_secret_key: str = Field(
        description="MinIO 시크릿 키"
    )
    minio_region: str = Field(
        default="us-east-1",
        description="MinIO 리전"
    )

    # 위키 데이터 설정
    wiki_data_dir: str = Field(
        description="위키 데이터가 있는 디렉토리 경로"
    )
    wiki_bucket_name: str = Field(
        description="위키 데이터를 저장할 MinIO 버킷명"
    )

    # 업로드 성능 설정
    _DEFAULT_CHUNK_SIZE = 25 * 1024 * 1024  # 25MB
    multipart_threshold: int = Field(
        default=_DEFAULT_CHUNK_SIZE,
        description="멀티파트 업로드 시작 임계값 (bytes)"
    )
    multipart_chunksize: int = Field(
        default=_DEFAULT_CHUNK_SIZE,
        description="멀티파트 업로드 청크 크기 (bytes)"
    )
    max_concurrency: int = Field(
        default=10,
        description="최대 동시 업로드 수"
    )
    use_threads: bool = Field(
        default=True,
        description="멀티스레딩 사용 여부"
    )

    # 재시도 설정
    max_retries: int = Field(
        default=3,
        description="최대 재시도 횟수"
    )
    retry_delay: int = Field(
        default=5,
        description="재시도 대기 시간 (초)"
    )

    # X API 설정
    x_api_key: str = Field(
        description="X API Key"
    )
    x_api_secret: str = Field(
        description="X API Secret"
    )
    x_bearer_token: str = Field(
        description="X Bearer Token"
    )
    x_access_token: str = Field(
        default="",
        description="X Access Token"
    )
    x_access_token_secret: str = Field(
        default="",
        description="X Access Token Secret"
    )
    x_api_version: str = Field(
        default="2",
        description="X API Version"
    )
    x_max_results: int = Field(
        default=100,
        description="X API 최대 결과 수"
    )
    x_rate_limit_wait: bool = Field(
        default=True,
        description="X API Rate Limit 대기 여부"
    )

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,  # 환경변수명 대소문자 구분 안함
        extra='ignore'  # 정의되지 않은 환경변수 무시
    )


# 전역 설정 인스턴스
settings = Settings()
