"""외부 의존성 통합 모듈

이 모듈은 외부 서비스들(Kafka, MinIO 등)에 대한 통합 인터페이스를 제공합니다.
각 서비스는 독립적으로 사용할 수 있지만, 이 모듈을 통해 일관된 방식으로 접근할 수 있습니다.
"""

__all__ = [
    "get_external_services_status",
]


def get_external_services_status():
    """모든 외부 서비스의 상태를 확인합니다."""
    status = {
        "kafka": {"connected": False, "error": None},
        "minio": {"connected": False, "error": None}
    }

    # Kafka 연결 확인 (지연 import)
    try:
        from .kafka import KafkaClient  # type: ignore
        try:
            kafka_client = KafkaClient()
            status["kafka"]["connected"] = kafka_client.test_connection()
        except Exception as e:
            status["kafka"]["error"] = str(e)
    except Exception as e:
        status["kafka"]["error"] = f"import error: {e}"

    # MinIO 연결 확인 (지연 import)
    try:
        from .minio import MinIOClient  # type: ignore
        try:
            with MinIOClient() as minio_client:
                minio_client.list_buckets()
            status["minio"]["connected"] = True
        except Exception as e:
            status["minio"]["error"] = str(e)
    except Exception as e:
        status["minio"]["error"] = f"import error: {e}"

    return status
