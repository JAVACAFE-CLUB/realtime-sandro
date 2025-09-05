"""위키 데이터 수집 서비스"""

import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any

from .minio_service import MinIOService
from .upload_service import UploadService
from ..core.config import settings

logger = logging.getLogger(__name__)


class WikiService:
    """위키 데이터 수집을 담당하는 서비스 클래스"""
    
    def __init__(self):
        self.upload_service = UploadService()
        self.minio_service = MinIOService()
    
    def get_available_files(self) -> Dict[str, Any]:
        """업로드 가능한 위키 파일 목록 조회"""
        data_dir = Path(settings.wiki_data_dir)
        
        if not data_dir.exists():
            raise FileNotFoundError(f"Data directory not found: {data_dir}")
        
        bz2_files = list(data_dir.glob("*.bz2"))
        
        files = []
        for file_path in bz2_files:
            size_mb = file_path.stat().st_size / (1024 * 1024)
            files.append({
                "name": file_path.name,
                "size_mb": round(size_mb, 2),
                "path": str(file_path)
            })
        
        return {
            "total": len(files),
            "files": files,
            "directory": str(data_dir)
        }
    
    def upload_all_files(self) -> Dict[str, int]:
        """모든 위키 파일을 업로드"""
        logger.info("Starting upload of all wiki files")
        return self.upload_service.upload_all_files()
    
    def upload_single_file(self, filename: str) -> bool:
        """특정 위키 파일 업로드"""
        data_dir = Path(settings.wiki_data_dir)
        file_path = data_dir / filename
        
        if not file_path.exists() or not filename.endswith('.bz2'):
            raise FileNotFoundError(f"File not found: {filename}")
        
        logger.info(f"Starting upload of single file: {filename}")
        return self.upload_service.upload_file_with_retry(file_path)
    
    def get_buckets(self) -> List[Dict[str, Any]]:
        """MinIO 버킷 목록 조회"""
        buckets = self.minio_service.list_buckets()
        
        bucket_list = []
        for bucket in buckets:
            # 버킷 내 객체 수 가져오기
            objects = self.minio_service.list_objects(bucket['name'], prefix='raw/')
            bucket_list.append({
                "name": bucket['name'],
                "creation_date": bucket['creation_date'],
                "object_count": len(objects) if objects else 0
            })
        
        return bucket_list
    
    def create_bucket(self, bucket_name: str) -> bool:
        """새 버킷 생성"""
        return self.minio_service.ensure_bucket_exists(bucket_name)