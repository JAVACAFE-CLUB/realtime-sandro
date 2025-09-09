"""헬스체크 엔드포인트"""

from datetime import datetime
from typing import Dict, Any

from fastapi import APIRouter, status

from ...core.config import settings
from ...external.kafka import KafkaClient
from ...external.minio import MinIOClient

router = APIRouter()


@router.get("/health", status_code=status.HTTP_200_OK)
async def health_check() -> Dict[str, Any]:
    """서버 헬스체크 엔드포인트"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "harvest-data-collector",
        "version": "0.1.0",
    }


@router.get("/status", status_code=status.HTTP_200_OK)
async def service_status() -> Dict[str, Any]:
    """서비스 상태 확인 엔드포인트"""
    status_info = {
        "timestamp": datetime.now().isoformat(),
        "minio": {"connected": False},
        "kafka": {"connected": False},
        "configuration": {
            "wiki_data_dir": str(settings.wiki_data_dir),
            "wiki_bucket_name": settings.wiki_bucket_name,
            "log_level": settings.log_level,
        }
    }

    # MinIO 연결 상태 확인
    try:
        # 버킷 리스트를 가져와서 연결 확인
        with MinIOClient() as minio_client:
            buckets = minio_client.list_buckets()
        status_info["minio"]["connected"] = True
        status_info["minio"]["buckets_count"] = len(buckets) if buckets else 0
    except Exception as e:
        status_info["minio"]["error"] = str(e)

    # 카프카 연결 상태 확인
    try:
        kafka_service = KafkaClient()
        kafka_connected = kafka_service.test_connection()
        status_info["kafka"]["connected"] = kafka_connected
        if not kafka_connected:
            status_info["kafka"]["error"] = "Connection test failed"
    except Exception as e:
        status_info["kafka"]["error"] = str(e)

    return status_info
