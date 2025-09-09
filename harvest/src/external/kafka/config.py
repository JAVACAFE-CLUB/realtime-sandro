"""깔끔하고 직관적인 타입 안전한 카프카 설정"""

import os
from pathlib import Path
from typing import Dict, Any

import yaml
from pydantic import ValidationError

from .config_models import (
    KafkaTopicsConfig,
    EnvironmentConfig,
    TopicSettings
)


def _find_config_dir() -> Path:
    """kafka-config 디렉토리 찾기"""
    current = Path(__file__).parent

    for _ in range(5):  # 최대 5단계까지 상위 디렉토리 탐색
        kafka_config_dir = current / "kafka-config"
        if kafka_config_dir.exists():
            return kafka_config_dir
        current = current.parent

    raise FileNotFoundError("kafka-config 디렉토리를 찾을 수 없습니다")


class KafkaConfig:
    """타입 안전한 카프카 설정 관리"""

    def __init__(self, environment: str = "local"):
        """
        카프카 설정 초기화
        
        Args:
            environment: 환경 설정 (local, dev, prod)
        
        Raises:
            ValidationError: 설정 파일 유효성 검증 실패
            FileNotFoundError: 설정 파일을 찾을 수 없음
        """
        self.environment = environment

        # kafka-config 디렉토리 찾기
        self.config_dir = _find_config_dir()

        # 설정 로드 및 검증
        self._load_configs()

    def _load_configs(self):
        """설정 파일 로드 및 Pydantic 검증"""
        try:
            # topics.yaml 로드
            topics_file = self.config_dir / "topics.yaml"
            with open(topics_file, 'r', encoding='utf-8') as f:
                topics_data = yaml.safe_load(f)
            self.topics = KafkaTopicsConfig(**topics_data)

            # 환경별 설정 로드
            env_file = self.config_dir / "environments" / f"{self.environment}.yaml"
            if env_file.exists():
                with open(env_file, 'r', encoding='utf-8') as f:
                    env_data = yaml.safe_load(f)
                self.env = EnvironmentConfig(**env_data)
            else:
                # 기본 환경 설정
                self.env = EnvironmentConfig(
                    kafka={'bootstrap-servers': 'localhost:9092'}
                )

        except ValidationError as e:
            raise ValidationError(f"설정 검증 실패 ({self.environment}): {e}") from e
        except yaml.YAMLError as e:
            raise ValueError(f"YAML 파싱 실패: {e}") from e

    # === 브로커 연결 ===

    @property
    def bootstrap_servers(self) -> str:
        """브로커 서버 주소"""
        return self.env.kafka.bootstrap_servers

    # === 토픽 관리 ===

    def topic_name(self, logical_name: str) -> str:
        """논리적 토픽명 → 실제 토픽명"""
        if logical_name not in self.topics.topics:
            available = list(self.topics.topics.keys())
            raise ValueError(f"알 수 없는 토픽: '{logical_name}'. 사용 가능: {available}")
        return self.topics.topics[logical_name].name

    def topic_settings(self, logical_name: str) -> TopicSettings:
        """토픽 설정 정보 (환경별 오버라이드 적용됨)"""
        if logical_name not in self.topics.topics:
            available = list(self.topics.topics.keys())
            raise ValueError(f"알 수 없는 토픽: '{logical_name}'. 사용 가능: {available}")

        settings = self.topics.topics[logical_name]

        # 환경별 오버라이드 적용
        if logical_name in self.env.topic_overrides:
            override = self.env.topic_overrides[logical_name]
            settings_dict = settings.model_dump()

            if override.partitions is not None:
                settings_dict['partitions'] = override.partitions
            if override.replication_factor is not None:
                settings_dict['replication_factor'] = override.replication_factor

            return TopicSettings(**settings_dict)

        return settings

    @property
    def all_topics(self) -> Dict[str, str]:
        """모든 토픽의 논리명 → 실제명 매핑"""
        return {
            logical_name: settings.name
            for logical_name, settings in self.topics.topics.items()
        }

    def topic_exists(self, logical_name: str) -> bool:
        """토픽 존재 여부"""
        return logical_name in self.topics.topics

    # === 프로듀서 설정 ===

    def producer_config(self) -> Dict[str, Any]:
        """프로듀서 설정 (confluent-kafka 호환)"""
        config = self.topics.producer_defaults.model_dump(by_alias=True)

        # 환경별 오버라이드
        if self.env.kafka.producer:
            overrides = self.env.kafka.producer.model_dump(by_alias=True, exclude_none=True)
            config.update(overrides)

        # 브로커 서버 추가
        config["bootstrap.servers"] = self.bootstrap_servers
        return config

    # === 컨슈머 설정 ===

    def consumer_config(self, group_id: str) -> Dict[str, Any]:
        """컨슈머 설정 (confluent-kafka 호환)"""
        if not group_id or not group_id.strip():
            raise ValueError("group_id는 빈 문자열일 수 없습니다")

        config = self.topics.consumer_defaults.model_dump(by_alias=True)

        # 환경별 오버라이드
        if self.env.kafka.consumer:
            overrides = self.env.kafka.consumer.model_dump(by_alias=True, exclude_none=True)
            config.update(overrides)

        # 필수 설정
        config["bootstrap.servers"] = self.bootstrap_servers
        config["group.id"] = group_id.strip()
        return config

    # === 스키마 관리 ===

    def schema_version(self, logical_name: str) -> str:
        """토픽 스키마 버전"""
        return self.topics.schema_versions.get(logical_name, "1.0")

    # === 디버깅/모니터링 ===

    @property
    def summary(self) -> Dict[str, Any]:
        """설정 요약 정보"""
        return {
            "environment": self.environment,
            "bootstrap_servers": self.bootstrap_servers,
            "topics_count": len(self.topics.topics),
            "topics": list(self.topics.topics.keys()),
            "schema_versions": dict(self.topics.schema_versions),
            "overrides": list(self.env.topic_overrides.keys()),
            "type_safe": True
        }


# === 전역 인스턴스 ===

# 환경변수에서 환경 설정 읽기
_environment = os.getenv("KAFKA_ENV", "local")

try:
    kafka_config = KafkaConfig(_environment)
except Exception as e:
    # 설정 로드 실패 시 에러 발생 (조용히 실패하지 않음)
    raise RuntimeError(f"카프카 설정 로드 실패: {e}") from e
