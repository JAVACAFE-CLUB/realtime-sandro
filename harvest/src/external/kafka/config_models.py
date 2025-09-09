"""Pydantic 기반 타입 안전한 카프카 설정 모델들"""

from enum import Enum
from typing import Dict, Optional, Union

from pydantic import BaseModel, Field, field_validator, model_validator, ConfigDict


class CleanupPolicy(str, Enum):
    """카프카 토픽 정리 정책"""
    DELETE = "delete"
    COMPACT = "compact"
    COMPACT_DELETE = "compact,delete"


class AcksType(str, Enum):
    """프로듀서 확인 응답 타입"""
    NONE = "0"
    LEADER = "1"
    ALL = "all"


class AutoOffsetReset(str, Enum):
    """컨슈머 오프셋 리셋 정책"""
    EARLIEST = "earliest"
    LATEST = "latest"
    NONE = "none"


class TopicSettings(BaseModel):
    """개별 토픽 설정 모델"""
    name: str = Field(..., description="실제 토픽명")
    partitions: int = Field(..., gt=0, description="파티션 수 (1 이상)")
    replication_factor: int = Field(
        ...,
        alias="replication-factor",
        ge=1,
        description="복제 팩터 (1 이상)"
    )
    configs: Dict[str, Union[str, int]] = Field(
        default_factory=dict,
        description="토픽별 추가 설정"
    )
    description: str = Field(default="", description="토픽 설명")

    @field_validator('configs', mode='before')
    @classmethod
    def validate_configs(cls, v):
        """토픽 설정 검증"""
        if not isinstance(v, dict):
            return {}

        # 문자열 키 검증
        validated = {}
        for key, value in v.items():
            if not isinstance(key, str):
                raise ValueError(f"토픽 설정 키는 문자열이어야 합니다: {key}")
            validated[key] = value

        return validated

    model_config = ConfigDict(populate_by_name=True)


class ProducerDefaults(BaseModel):
    """프로듀서 기본 설정 모델"""
    acks: AcksType = Field(default=AcksType.ALL, description="확인 응답 설정")
    enable_idempotence: bool = Field(
        default=True,
        alias="enable.idempotence",
        description="멱등성 보장"
    )
    retries: int = Field(default=3, ge=0, description="재시도 횟수")
    retry_backoff_ms: int = Field(
        default=1000,
        alias="retry.backoff.ms",
        ge=0,
        description="재시도 대기시간(ms)"
    )

    # 성능 최적화 설정
    batch_size: int = Field(
        default=16384,
        alias="batch.size",
        gt=0,
        description="배치 크기(bytes)"
    )
    linger_ms: int = Field(
        default=100,
        alias="linger.ms",
        ge=0,
        description="배치 대기시간(ms)"
    )
    compression_type: str = Field(
        default="gzip",
        alias="compression.type",
        description="압축 타입"
    )
    max_in_flight_requests_per_connection: int = Field(
        default=5,
        alias="max.in.flight.requests.per.connection",
        gt=0,
        description="연결당 최대 동시 요청 수"
    )

    @field_validator('compression_type')
    @classmethod
    def validate_compression_type(cls, v):
        """압축 타입 검증"""
        valid_types = {"none", "gzip", "snappy", "lz4", "zstd"}
        if v not in valid_types:
            raise ValueError(f"지원되지 않는 압축 타입: {v}. 사용 가능: {valid_types}")
        return v

    model_config = ConfigDict(populate_by_name=True)


class ConsumerDefaults(BaseModel):
    """컨슈머 기본 설정 모델"""
    enable_auto_commit: bool = Field(
        default=False,
        alias="enable.auto.commit",
        description="자동 커밋 사용"
    )
    auto_offset_reset: AutoOffsetReset = Field(
        default=AutoOffsetReset.EARLIEST,
        alias="auto.offset.reset",
        description="오프셋 리셋 정책"
    )
    max_poll_records: int = Field(
        default=500,
        alias="max.poll.records",
        gt=0,
        description="한 번에 폴링할 최대 레코드 수"
    )
    session_timeout_ms: int = Field(
        default=30000,
        alias="session.timeout.ms",
        gt=0,
        description="세션 타임아웃(ms)"
    )
    heartbeat_interval_ms: int = Field(
        default=10000,
        alias="heartbeat.interval.ms",
        gt=0,
        description="하트비트 간격(ms)"
    )

    @model_validator(mode='after')
    def validate_timeout_consistency(self):
        """타임아웃 설정 일관성 검증"""
        session_timeout = self.session_timeout_ms
        heartbeat_interval = self.heartbeat_interval_ms

        if heartbeat_interval >= session_timeout:
            raise ValueError(
                f"heartbeat.interval.ms({heartbeat_interval})는 "
                f"session.timeout.ms({session_timeout})보다 작아야 합니다"
            )

        # 일반적으로 heartbeat는 session timeout의 1/3 이하 권장
        if heartbeat_interval > session_timeout // 3:
            import warnings
            warnings.warn(
                f"heartbeat.interval.ms는 session.timeout.ms의 1/3 이하로 설정하는 것이 권장됩니다. "
                f"현재: {heartbeat_interval}ms > {session_timeout // 3}ms"
            )

        return self

    model_config = ConfigDict(populate_by_name=True)


class KafkaTopicsConfig(BaseModel):
    """topics.yaml 전체 구조 모델"""
    topics: Dict[str, TopicSettings] = Field(
        ...,
        description="토픽 정의 맵"
    )
    producer_defaults: ProducerDefaults = Field(
        ...,
        alias="producer-defaults",
        description="프로듀서 기본 설정"
    )
    consumer_defaults: ConsumerDefaults = Field(
        ...,
        alias="consumer-defaults",
        description="컨슈머 기본 설정"
    )
    schema_versions: Dict[str, str] = Field(
        default_factory=dict,
        alias="schema-versions",
        description="스키마 버전 맵"
    )

    @field_validator('topics')
    @classmethod
    def validate_topics_not_empty(cls, v):
        """토픽이 최소 하나는 있어야 함"""
        if not v:
            raise ValueError("최소 하나의 토픽은 정의되어야 합니다")
        return v

    @model_validator(mode='after')
    def validate_schema_versions(self):
        """스키마 버전과 토픽 일관성 검증"""
        if self.topics and self.schema_versions:
            for logical_name in self.schema_versions.keys():
                if logical_name not in self.topics:
                    raise ValueError(
                        f"스키마 버전이 정의된 토픽 '{logical_name}'이 "
                        f"토픽 정의에 없습니다"
                    )
        return self

    model_config = ConfigDict(populate_by_name=True)


class TopicOverrides(BaseModel):
    """환경별 토픽 오버라이드 설정"""
    partitions: Optional[int] = Field(None, gt=0, description="파티션 수 오버라이드")
    replication_factor: Optional[int] = Field(
        None,
        alias="replication-factor",
        ge=1,
        description="복제 팩터 오버라이드"
    )

    model_config = ConfigDict(populate_by_name=True)


class ProducerOverrides(BaseModel):
    """환경별 프로듀서 설정 오버라이드"""
    acks: Optional[AcksType] = None
    linger_ms: Optional[int] = Field(None, alias="linger.ms", ge=0)
    batch_size: Optional[int] = Field(None, alias="batch.size", gt=0)

    model_config = ConfigDict(populate_by_name=True)


class ConsumerOverrides(BaseModel):
    """환경별 컨슈머 설정 오버라이드"""
    max_poll_records: Optional[int] = Field(None, alias="max.poll.records", gt=0)
    session_timeout_ms: Optional[int] = Field(None, alias="session.timeout.ms", gt=0)

    model_config = ConfigDict(populate_by_name=True)


class KafkaEnvironmentSettings(BaseModel):
    """카프카 환경별 설정"""
    bootstrap_servers: str = Field(
        ...,
        alias="bootstrap-servers",
        description="브로커 서버 목록"
    )
    producer: Optional[ProducerOverrides] = Field(
        None,
        description="프로듀서 설정 오버라이드"
    )
    consumer: Optional[ConsumerOverrides] = Field(
        None,
        description="컨슈머 설정 오버라이드"
    )

    @field_validator('bootstrap_servers')
    @classmethod
    def validate_bootstrap_servers(cls, v):
        """브로커 서버 주소 형식 검증"""
        if not v.strip():
            raise ValueError("브로커 서버 주소는 빈 문자열일 수 없습니다")

        # 기본적인 host:port 형식 검증
        servers = [s.strip() for s in v.split(',') if s.strip()]
        if not servers:
            raise ValueError("최소 하나의 브로커 서버는 필요합니다")

        for server in servers:
            if ':' not in server:
                raise ValueError(
                    f"브로커 서버는 'host:port' 형식이어야 합니다: {server}"
                )

            host, port_str = server.rsplit(':', 1)
            if not host:
                raise ValueError(f"호스트명이 비어있습니다: {server}")

            try:
                port = int(port_str)
                if not (1 <= port <= 65535):
                    raise ValueError(f"유효하지 않은 포트 번호: {port}")
            except ValueError:
                raise ValueError(f"포트는 숫자여야 합니다: {port_str}")

        return v

    model_config = ConfigDict(populate_by_name=True)


class LoggingConfig(BaseModel):
    """로깅 설정"""
    level: str = Field(default="INFO", description="로그 레벨")

    @field_validator('level')
    @classmethod
    def validate_log_level(cls, v):
        """로그 레벨 검증"""
        valid_levels = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}
        v_upper = v.upper()
        if v_upper not in valid_levels:
            raise ValueError(f"유효하지 않은 로그 레벨: {v}. 사용 가능: {valid_levels}")
        return v_upper


class EnvironmentConfig(BaseModel):
    """환경별 설정 파일 구조 모델"""
    kafka: KafkaEnvironmentSettings = Field(..., description="카프카 설정")
    logging: LoggingConfig = Field(
        default_factory=LoggingConfig,
        description="로깅 설정"
    )
    topic_overrides: Dict[str, TopicOverrides] = Field(
        default_factory=dict,
        alias="topic-overrides",
        description="토픽별 오버라이드 설정"
    )

    model_config = ConfigDict(populate_by_name=True)
