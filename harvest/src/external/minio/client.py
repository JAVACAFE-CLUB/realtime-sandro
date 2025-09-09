"""타입 안전한 MinIO 클라이언트"""

import logging
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Optional, Callable, Dict, Any, List

import boto3
from boto3.s3.transfer import TransferConfig
from botocore.exceptions import ClientError, NoCredentialsError

from .exceptions import MinIOConnectionError, MinIOOperationError, MinIOBucketError
from ...core.config import settings

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


def _get_error_code(error: ClientError) -> int:
    """ClientError에서 HTTP 상태 코드 추출"""
    return int(error.response['Error']['Code'])


class MinIOClient:
    """타입 안전한 MinIO 클라이언트 클래스
    
    Context manager를 지원하여 리소스 관리를 안전하게 처리합니다.
    """

    def __init__(self):
        """
        MinIO 클라이언트 초기화
        
        core.config.settings를 사용하여 초기화합니다.
        """
        self.config = settings
        self._validate_config()
        self._initialize_client()

    def _validate_config(self) -> None:
        """MinIO 설정 유효성 검사"""
        if not hasattr(self.config, 'minio_endpoint'):
            raise ValueError("MinIO endpoint가 설정되지 않았습니다")
        if not hasattr(self.config, 'minio_access_key'):
            raise ValueError("MinIO access_key가 설정되지 않았습니다")
        if not hasattr(self.config, 'minio_secret_key'):
            raise ValueError("MinIO secret_key가 설정되지 않았습니다")

    def __enter__(self):
        """Context manager 진입"""
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager 종료 - 리소스 정리"""
        self.close()

    def close(self):
        """클라이언트 연결 정리"""
        try:
            if hasattr(self, 's3_client') and self.s3_client:
                # boto3 클라이언트는 명시적 close가 필요하지 않지만 정리 작업
                logger.info("MinIO 클라이언트 연결 정리 완료")
        except Exception as e:
            logger.error(f"MinIO 클라이언트 정리 중 오류: {e}")

    def _initialize_client(self):
        """boto3 S3 클라이언트 초기화"""
        try:
            boto3_config = {
                "endpoint_url": self.config.minio_endpoint,
                "aws_access_key_id": self.config.minio_access_key,
                "aws_secret_access_key": self.config.minio_secret_key,
                "region_name": self.config.minio_region
            }
            self.s3_client = boto3.client('s3', **boto3_config)
            logger.info(f"MinIO 클라이언트 초기화 완료: {self.config.minio_endpoint}")
        except NoCredentialsError as e:
            raise MinIOConnectionError("MinIO 자격증명을 찾을 수 없습니다") from e
        except Exception as e:
            raise MinIOConnectionError(f"MinIO 클라이언트 초기화 실패: {e}") from e

    def ensure_bucket(self, bucket_name: str) -> bool:
        """
        버킷이 존재하지 않으면 생성
        
        Args:
            bucket_name: 생성할 버킷명
            
        Returns:
            생성 또는 이미 존재하는 경우 True
            
        Raises:
            MinIOBucketError: 버킷 생성/확인 실패
        """
        try:
            self.s3_client.head_bucket(Bucket=bucket_name)
            logger.info(f"버킷 '{bucket_name}'이 이미 존재합니다.")
            return True
        except ClientError as e:
            error_code = _get_error_code(e)
            if error_code == 404:
                # 버킷이 존재하지 않으므로 생성 (core 설정에는 auto_create_bucket이 없으므로 기본적으로 생성)

                try:
                    self.s3_client.create_bucket(Bucket=bucket_name)
                    logger.info(f"버킷 '{bucket_name}' 생성 완료")
                    return True
                except ClientError as create_error:
                    raise MinIOBucketError(f"버킷 생성 실패: {create_error}") from create_error
            else:
                raise MinIOBucketError(f"버킷 확인 중 오류 발생: {e}") from e

    def upload_file(
            self,
            file_path: str,
            bucket_name: Optional[str] = None,
            object_key: Optional[str] = None,
            extra_args: Optional[Dict[str, Any]] = None,
            progress_callback: Optional[Callable[[UploadProgress], None]] = None
    ) -> bool:
        """
        파일을 MinIO에 업로드 (재시도 포함)
        
        Args:
            file_path: 업로드할 로컬 파일 경로
            bucket_name: 대상 버킷명 (None이면 기본 버킷 사용)
            object_key: 객체 키 (None이면 파일명 사용)
            extra_args: 추가 메타데이터
            progress_callback: 진행 상황 콜백 함수
            
        Returns:
            업로드 성공 시 True
            
        Raises:
            MinIOOperationError: 업로드 실패
        """
        file_path = Path(file_path)
        if not file_path.exists():
            raise MinIOOperationError(f"파일이 존재하지 않습니다: {file_path}")

        bucket_name = bucket_name or self.config.wiki_bucket_name
        object_key = object_key or file_path.name

        # 버킷 생성 확인
        self.ensure_bucket(bucket_name)

        # 재시도 로직으로 업로드 수행
        return self._upload_with_retry(file_path, bucket_name, object_key, extra_args, progress_callback)

    def _upload_with_retry(
            self,
            file_path: Path,
            bucket_name: str,
            object_key: str,
            extra_args: Optional[Dict[str, Any]],
            progress_callback: Optional[Callable[[UploadProgress], None]]
    ) -> bool:
        """재시도 로직이 포함된 업로드"""
        file_size = file_path.stat().st_size
        for attempt in range(self.config.max_retries + 1):
            try:
                return self._perform_upload(file_path, bucket_name, object_key, extra_args, progress_callback, file_size)
            except Exception as e:
                if attempt == self.config.max_retries:
                    raise MinIOOperationError(f"파일 업로드 실패 (최대 재시도 초과): {e}") from e

                # 재시도 대기 (backoff_factor는 core 설정에 없으므로 기본값 2.0 사용)
                delay = self.config.retry_delay * (2.0 ** attempt)
                logger.warning(f"업로드 재시도 {attempt + 1}/{self.config.max_retries}, {delay:.1f}초 후 재시도: {e}")
                time.sleep(delay)

        return False  # 이 줄은 도달하지 않아야 함

    def _perform_upload(
            self,
            file_path: Path,
            bucket_name: str,
            object_key: str,
            extra_args: Optional[Dict[str, Any]],
            progress_callback: Optional[Callable[[UploadProgress], None]],
            file_size: int
    ) -> bool:
        """실제 업로드 수행"""
        # 진행률 콜백 설정
        callback = None
        if progress_callback:
            def callback(bytes_transferred):
                progress = UploadProgress(
                    filename=file_path.name,
                    bytes_transferred=bytes_transferred,
                    total_bytes=file_size
                )
                progress_callback(progress)

        # 전송 설정
        transfer_config = TransferConfig(
            multipart_threshold=self.config.multipart_threshold,
            max_concurrency=self.config.max_concurrency,
            multipart_chunksize=self.config.multipart_chunksize,
            use_threads=self.config.use_threads
        )

        # 업로드 수행
        self.s3_client.upload_file(
            str(file_path),
            bucket_name,
            object_key,
            ExtraArgs=extra_args,
            Callback=callback,
            Config=transfer_config
        )

        logger.info(f"파일 업로드 완료: {object_key}")
        return True

    def list_objects(self, bucket_name: Optional[str] = None, prefix: str = "") -> List[str]:
        """
        버킷의 객체 목록 조회
        
        Args:
            bucket_name: 조회할 버킷명 (None이면 기본 버킷 사용)
            prefix: 객체 키 접두사
            
        Returns:
            객체 목록
            
        Raises:
            MinIOOperationError: 목록 조회 실패
        """
        bucket_name = bucket_name or self.config.wiki_bucket_name

        try:
            response = self.s3_client.list_objects_v2(
                Bucket=bucket_name,
                Prefix=prefix
            )
            return [obj['Key'] for obj in response.get('Contents', [])]
        except ClientError as e:
            raise MinIOOperationError(f"객체 목록 조회 실패: {e}") from e

    def object_exists(self, object_key: str, bucket_name: Optional[str] = None) -> bool:
        """
        객체 존재 여부 확인
        
        Args:
            object_key: 객체 키
            bucket_name: 버킷명 (None이면 기본 버킷 사용)
            
        Returns:
            존재하면 True
            
        Raises:
            MinIOOperationError: 확인 중 오류 (404 제외)
        """
        bucket_name = bucket_name or self.config.wiki_bucket_name

        try:
            self.s3_client.head_object(Bucket=bucket_name, Key=object_key)
            return True
        except ClientError as e:
            if _get_error_code(e) == 404:
                return False
            else:
                raise MinIOOperationError(f"객체 확인 중 오류: {e}") from e

    def object_info(self, object_key: str, bucket_name: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """
        객체 정보 조회
        
        Args:
            object_key: 객체 키
            bucket_name: 버킷명 (None이면 기본 버킷 사용)
            
        Returns:
            객체 정보 딕셔너리 또는 None (존재하지 않는 경우)
            
        Raises:
            MinIOOperationError: 조회 실패 (404 제외)
        """
        bucket_name = bucket_name or self.config.wiki_bucket_name

        try:
            response = self.s3_client.head_object(Bucket=bucket_name, Key=object_key)
            return {
                'size': response['ContentLength'],
                'last_modified': response['LastModified'],
                'etag': response['ETag'].strip('"'),
                'content_type': response.get('ContentType', 'binary/octet-stream')
            }
        except ClientError as e:
            if _get_error_code(e) == 404:
                return None
            else:
                raise MinIOOperationError(f"객체 정보 조회 실패: {e}") from e

    def list_buckets(self) -> List[Dict[str, Any]]:
        """
        모든 버킷 목록 조회
        
        Returns:
            버킷 목록
            
        Raises:
            MinIOOperationError: 목록 조회 실패
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
            raise MinIOOperationError(f"버킷 목록 조회 실패: {e}") from e

    def create_bucket(self, bucket_name: str) -> bool:
        """
        새 버킷 생성
        
        Args:
            bucket_name: 생성할 버킷명
            
        Returns:
            생성 성공 시 True
            
        Raises:
            MinIOBucketError: 버킷 생성 실패
        """
        try:
            self.s3_client.create_bucket(Bucket=bucket_name)
            logger.info(f"버킷 '{bucket_name}' 생성 완료")
            return True
        except ClientError as e:
            raise MinIOBucketError(f"버킷 생성 실패: {e}") from e

    @property
    def default_bucket(self) -> str:
        """기본 버킷명 반환"""
        return self.config.wiki_bucket_name
