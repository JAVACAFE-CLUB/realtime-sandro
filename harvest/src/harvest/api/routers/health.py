"""헬스체크 엔드포인트"""

from datetime import datetime
from typing import Dict, Any

from fastapi import APIRouter, status

from ...minio_uploader import MinIOUploader
from ...core.config import settings

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
        "configuration": {
            "wiki_data_dir": str(settings.wiki_data_dir),
            "wiki_bucket_name": settings.wiki_bucket_name,
            "log_level": settings.log_level,
        }
    }
    
    # MinIO 연결 상태 확인
    try:
        uploader = MinIOUploader()
        # 버킷 리스트를 가져와서 연결 확인
        buckets = uploader.list_buckets()
        status_info["minio"]["connected"] = True
        status_info["minio"]["buckets_count"] = len(buckets) if buckets else 0
    except Exception as e:
        status_info["minio"]["error"] = str(e)
    
    return status_info