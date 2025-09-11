"""외부 의존성 통합 모듈

이 모듈은 외부 서비스들(Kafka, MinIO 등)에 대한 통합 인터페이스를 제공합니다.
각 서비스는 독립적으로 사용할 수 있지만, 이 모듈을 통해 일관된 방식으로 접근할 수 있습니다.
"""

# Kafka 관련 imports
from .kafka import (
    KafkaConfig,
    kafka_config,
    KafkaClient,
)

# MinIO 관련 imports
from .minio import (
    MinIOClient,
    UploadProgress,
    # 예외 클래스들
    MinIOError,
    MinIOConnectionError,
    MinIOBucketError,
    MinIOOperationError,
    MinIOConfigError,
    MinIOUploadError,
    MinIODownloadError,
)

# X API 관련 imports
from .x import (
    XAPIClient,
    get_x_api_client,
    XAPIError,
    XAPIConnectionError,
    XAPIAuthenticationError,
    XAPIRateLimitError,
    XAPIConfigError,
    XAPISearchError,
    XAPITimelineError,
)

__all__ = [
    # Kafka
    "KafkaConfig",
    "kafka_config",
    "KafkaClient",

    # MinIO
    "MinIOClient",
    "UploadProgress",

    # MinIO 예외 클래스들
    "MinIOError",
    "MinIOConnectionError",
    "MinIOBucketError",
    "MinIOOperationError",
    "MinIOConfigError",
    "MinIOUploadError",
    "MinIODownloadError",
    # X API
    "XAPIClient",
    "get_x_api_client",
    "XAPIError",
    "XAPIConnectionError",
    "XAPIAuthenticationError",
    "XAPIRateLimitError",
    "XAPIConfigError",
    "XAPISearchError",
    "XAPITimelineError",
]


def get_external_services_status():
    """모든 외부 서비스의 상태를 확인합니다."""
    status = {
        "kafka": {"connected": False, "error": None},
        "minio": {"connected": False, "error": None}
    }

    # Kafka 연결 확인
    try:
        kafka_client = KafkaClient()
        status["kafka"]["connected"] = kafka_client.test_connection()
    except Exception as e:
        status["kafka"]["error"] = str(e)

    # MinIO 연결 확인  
    try:
        with MinIOClient() as minio_client:
            minio_client.list_buckets()
        status["minio"]["connected"] = True
    except Exception as e:
        status["minio"]["error"] = str(e)

    return status
