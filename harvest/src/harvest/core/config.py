"""Pydantic 기반 설정 관리"""

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 설정 클래스"""
    
    # MinIO 연결 설정
    minio_endpoint: str = Field(
        default="http://localhost:9000",
        description="MinIO 서버 엔드포인트"
    )
    minio_access_key: str = Field(
        default="minio",
        description="MinIO 액세스 키"
    )
    minio_secret_key: str = Field(
        default="minio123",
        description="MinIO 시크릿 키"
    )
    minio_region: str = Field(
        default="us-east-1",
        description="MinIO 리전"
    )
    
    # 위키 데이터 설정
    wiki_data_dir: str = Field(
        default="/Users/sandeulpark/bigdata",
        description="위키 데이터가 있는 디렉토리 경로"
    )
    wiki_bucket_name: str = Field(
        default="wiki-data",
        description="위키 데이터를 저장할 MinIO 버킷명"
    )
    
    # 업로드 성능 설정
    multipart_threshold: int = Field(
        default=26214400,  # 25MB
        description="멀티파트 업로드 시작 임계값 (bytes)"
    )
    multipart_chunksize: int = Field(
        default=26214400,  # 25MB
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
    
    # 로그 설정
    log_file_path: str = Field(
        default="upload.log",
        description="로그 파일 경로"
    )
    log_level: str = Field(
        default="INFO",
        description="로그 레벨 (DEBUG, INFO, WARNING, ERROR, CRITICAL)"
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

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False  # 환경변수명 대소문자 구분 안함


# 전역 설정 인스턴스
settings = Settings()