"""타입 안전한 카프카 모듈"""

from .client import KafkaClient
# 새로운 깔끔한 API
from .config import KafkaConfig, kafka_config

__all__ = [
    # 새로운 API (권장)
    "KafkaConfig", "kafka_config", "KafkaClient"
]
