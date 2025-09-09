"""카프카 메시지 스키마 정의"""

from datetime import datetime
from enum import Enum
from typing import Dict, Any, Optional
from uuid import UUID

from pydantic import BaseModel, Field, ConfigDict


class DataType(str, Enum):
    """수집 데이터 타입"""
    WIKI_DUMP = "wiki-dump"
    NEWS_HTML = "news-html"
    API_JSON = "api-json"


class DataStatus(str, Enum):
    """데이터 처리 상태"""
    COLLECTED = "collected"
    PROCESSING = "processing"
    PROCESSED = "processed"
    FAILED = "failed"


class DataCollectedMessage(BaseModel):
    """데이터 수집 완료 메시지"""

    # 데이터 식별 정보
    data_id: UUID = Field(description="데이터 고유 ID")
    data_type: DataType = Field(description="데이터 타입")

    # MinIO 저장 정보
    bucket_name: str = Field(description="MinIO 버킷명")
    object_key: str = Field(description="MinIO 객체 키 (파일 경로)")

    # 파일 메타데이터
    original_filename: str = Field(description="원본 파일명")
    file_size: int = Field(description="파일 크기 (bytes)")
    content_type: str = Field(description="컨텐츠 타입")

    # 수집 메타데이터
    source: str = Field(description="데이터 소스 (bigdata, news-crawler, api 등)")
    collected_at: datetime = Field(description="수집 완료 시간")
    status: DataStatus = Field(default=DataStatus.COLLECTED, description="처리 상태")

    # 추가 메타데이터
    metadata: Dict[str, Any] = Field(default_factory=dict, description="추가 메타데이터")

    # 처리 옵션
    priority: int = Field(default=1, description="처리 우선순위 (1=낮음, 5=높음)")

    model_config = ConfigDict()


class DataRefinedMessage(BaseModel):
    """데이터 정제 완료 메시지"""

    # 기본 식별 정보
    data_id: UUID = Field(description="원본 데이터 ID (data-collected에서 전달받은)")
    refined_data_id: UUID = Field(description="정제된 데이터 고유 ID")
    data_type: DataType = Field(description="데이터 타입")

    # 정제 결과 정보
    bucket_name: str = Field(description="정제된 데이터가 저장된 버킷")
    object_key: str = Field(description="정제된 데이터 객체 키")
    refined_filename: str = Field(description="정제된 파일명")
    refined_size: int = Field(description="정제된 파일 크기 (bytes)")

    # 정제 메타데이터
    refinement_type: str = Field(description="정제 타입 (extract, clean, transform 등)")
    source_file: str = Field(description="원본 파일 경로/키")
    refined_at: datetime = Field(description="정제 완료 시간")
    status: DataStatus = Field(default=DataStatus.PROCESSED, description="정제 상태")

    # 처리 결과
    record_count: Optional[int] = Field(default=None, description="정제된 레코드 수")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="정제 관련 메타데이터")

    model_config = ConfigDict()


class KafkaMessageHeaders(BaseModel):
    """카프카 메시지 헤더"""

    message_type: str = Field(description="메시지 타입")
    version: str = Field(default="1.0", description="스키마 버전")
    source_service: str = Field(default="harvest", description="발신 서비스명")
    timestamp: datetime = Field(default_factory=datetime.now, description="메시지 생성 시간")

    model_config = ConfigDict()
