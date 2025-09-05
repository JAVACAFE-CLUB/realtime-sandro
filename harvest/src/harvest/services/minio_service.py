"""MinIO 업로더 클래스 - boto3를 사용하여 MinIO에 파일 업로드"""

import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Optional, Callable, Dict, Any, List

import boto3
from boto3.s3.transfer import TransferConfig
from botocore.exceptions import ClientError, NoCredentialsError

from ..core.config import settings

logger = logging.getLogger(__name__)


@dataclass
class UploadProgress:
    """업로드 진행 상황을 나타내는 데이터 클래스"""
    filename: str
    bytes_transferred: int
    total_bytes: int

    @property
    def progress_percent(self) -> float:
        """진행률을 백분율로 반환"""
        if self.total_bytes == 0:
            return 0.0
        return (self.bytes_transferred / self.total_bytes) * 100


class MinIOService:
    """boto3를 사용한 MinIO 서비스 클래스"""

    def __init__(
            self,
            endpoint_url: Optional[str] = None,
            aws_access_key_id: Optional[str] = None,
            aws_secret_access_key: Optional[str] = None,
            region_name: Optional[str] = None
    ):
        """
        MinIO 서비스 초기화
        
        Args:
            endpoint_url: MinIO 서버 엔드포인트 (None인 경우 설정에서 읽음)
            aws_access_key_id: MinIO 액세스 키 (None인 경우 설정에서 읽음)
            aws_secret_access_key: MinIO 시크릿 키 (None인 경우 설정에서 읽음)
            region_name: 리전명 (None인 경우 설정에서 읽음)
        """
        # 설정에서 값 읽기 (매개변수 우선)
        self.endpoint_url = endpoint_url or settings.minio_endpoint
        self.aws_access_key_id = aws_access_key_id or settings.minio_access_key
        self.aws_secret_access_key = aws_secret_access_key or settings.minio_secret_key
        self.region_name = region_name or settings.minio_region

        try:
            self.s3_client = boto3.client(
                's3',
                endpoint_url=self.endpoint_url,
                aws_access_key_id=self.aws_access_key_id,
                aws_secret_access_key=self.aws_secret_access_key,
                region_name=self.region_name
            )
            logger.info(f"MinIO 클라이언트 초기화 완료: {self.endpoint_url}")
        except NoCredentialsError:
            logger.error("MinIO 자격증명을 찾을 수 없습니다.")
            raise
        except Exception as e:
            logger.error(f"MinIO 클라이언트 초기화 실패: {e}")
            raise

    def ensure_bucket_exists(self, bucket_name: str) -> bool:
        """
        버킷이 존재하지 않으면 생성
        
        Args:
            bucket_name: 생성할 버킷명
            
        Returns:
            생성 또는 이미 존재하는 경우 True, 실패 시 False
        """
        try:
            self.s3_client.head_bucket(Bucket=bucket_name)
            logger.info(f"버킷 '{bucket_name}'이 이미 존재합니다.")
            return True
        except ClientError as e:
            error_code = int(e.response['Error']['Code'])
            if error_code == 404:
                # 버킷이 존재하지 않으므로 생성
                try:
                    self.s3_client.create_bucket(Bucket=bucket_name)
                    logger.info(f"버킷 '{bucket_name}' 생성 완료")
                    return True
                except ClientError as create_error:
                    logger.error(f"버킷 생성 실패: {create_error}")
                    return False
            else:
                logger.error(f"버킷 확인 중 오류 발생: {e}")
                return False

    def upload_file(
            self,
            file_path: str,
            bucket_name: str,
            object_key: Optional[str] = None,
            extra_args: Optional[Dict[str, Any]] = None,
            progress_callback: Optional[Callable[[UploadProgress], None]] = None
    ) -> bool:
        """
        파일을 MinIO에 업로드
        
        Args:
            file_path: 업로드할 로컬 파일 경로
            bucket_name: 대상 버킷명
            object_key: 객체 키 (None이면 파일명 사용)
            extra_args: 추가 메타데이터
            progress_callback: 진행 상황 콜백 함수
            
        Returns:
            업로드 성공 시 True, 실패 시 False
        """
        file_path = Path(file_path)
        if not file_path.exists():
            logger.error(f"파일이 존재하지 않습니다: {file_path}")
            return False

        if object_key is None:
            object_key = file_path.name

        # 버킷 생성 확인
        if not self.ensure_bucket_exists(bucket_name):
            return False

        # 파일 크기 확인
        file_size = file_path.stat().st_size
        logger.info(f"파일 업로드 시작: {file_path.name} ({file_size:,} bytes)")

        # 진행률 콜백 설정
        if progress_callback:
            def callback(bytes_transferred):
                progress = UploadProgress(
                    filename=file_path.name,
                    bytes_transferred=bytes_transferred,
                    total_bytes=file_size
                )
                progress_callback(progress)
        else:
            callback = None

        # 멀티파트 업로드 설정 (큰 파일용) - 설정에서 읽기
        config = TransferConfig(
            multipart_threshold=settings.multipart_threshold,
            max_concurrency=settings.max_concurrency,
            multipart_chunksize=settings.multipart_chunksize,
            use_threads=settings.use_threads
        )

        try:
            self.s3_client.upload_file(
                str(file_path),
                bucket_name,
                object_key,
                ExtraArgs=extra_args,
                Callback=callback,
                Config=config
            )
            logger.info(f"파일 업로드 완료: {object_key}")
            return True

        except ClientError as e:
            logger.error(f"파일 업로드 실패: {e}")
            return False
        except Exception as e:
            logger.error(f"예상치 못한 오류: {e}")
            return False

    def list_objects(self, bucket_name: str, prefix: str = "") -> List[str]:
        """
        버킷의 객체 목록 조회
        
        Args:
            bucket_name: 조회할 버킷명
            prefix: 객체 키 접두사
            
        Returns:
            객체 목록
        """
        try:
            response = self.s3_client.list_objects_v2(
                Bucket=bucket_name,
                Prefix=prefix
            )

            if 'Contents' in response:
                return [obj['Key'] for obj in response['Contents']]
            else:
                return []

        except ClientError as e:
            logger.error(f"객체 목록 조회 실패: {e}")
            return []

    def object_exists(self, bucket_name: str, object_key: str) -> bool:
        """
        객체 존재 여부 확인
        
        Args:
            bucket_name: 버킷명
            object_key: 객체 키
            
        Returns:
            존재하면 True, 그렇지 않으면 False
        """
        try:
            self.s3_client.head_object(Bucket=bucket_name, Key=object_key)
            return True
        except ClientError as e:
            if int(e.response['Error']['Code']) == 404:
                return False
            else:
                logger.error(f"객체 확인 중 오류: {e}")
                return False

    def get_object_info(self, bucket_name: str, object_key: str) -> Optional[Dict[str, Any]]:
        """
        객체 정보 조회
        
        Args:
            bucket_name: 버킷명
            object_key: 객체 키
            
        Returns:
            객체 정보 딕셔너리 또는 None
        """
        try:
            response = self.s3_client.head_object(Bucket=bucket_name, Key=object_key)
            return {
                'size': response['ContentLength'],
                'last_modified': response['LastModified'],
                'etag': response['ETag'].strip('"'),
                'content_type': response.get('ContentType', 'binary/octet-stream')
            }
        except ClientError as e:
            if int(e.response['Error']['Code']) == 404:
                return None
            else:
                logger.error(f"객체 정보 조회 실패: {e}")
                return None

    def list_buckets(self) -> List[Dict[str, Any]]:
        """
        모든 버킷 목록 조회
        
        Returns:
            버킷 목록
        """
        try:
            response = self.s3_client.list_buckets()
            return [
                {
                    'name': bucket['Name'],
                    'creation_date': bucket['CreationDate'].isoformat()
                }
                for bucket in response['Buckets']
            ]
        except ClientError as e:
            logger.error(f"버킷 목록 조회 실패: {e}")
            return []


