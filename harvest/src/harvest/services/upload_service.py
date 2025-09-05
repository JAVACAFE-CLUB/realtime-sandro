"""위키 데이터 파일을 MinIO에 업로드하는 서비스"""

import logging
import time
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Dict, Any

from .minio_service import MinIOService, UploadProgress
from ..core.config import settings

logger = logging.getLogger(__name__)


class UploadService:
    """데이터 업로드 관리 서비스 클래스"""

    def __init__(
        self, data_dir: Optional[str] = None, bucket_name: Optional[str] = None
    ):
        """
        업로드 서비스 초기화

        Args:
            data_dir: 데이터가 있는 디렉토리 (None인 경우 설정에서 읽음)
            bucket_name: MinIO 버킷명 (None인 경우 설정에서 읽음)
        """
        # 설정에서 값 읽기 (매개변수 우선)
        data_dir = data_dir or settings.wiki_data_dir
        bucket_name = bucket_name or settings.wiki_bucket_name

        self.data_dir = Path(data_dir)
        self.bucket_name = bucket_name
        self.minio_service = MinIOService()

        if not self.data_dir.exists():
            raise FileNotFoundError(f"데이터 디렉토리가 존재하지 않습니다: {data_dir}")

    def find_bz2_files(self) -> List[Path]:
        """
        .bz2 파일들을 찾아서 반환

        Returns:
            .bz2 파일 경로 목록
        """
        bz2_files = list(self.data_dir.glob("*.bz2"))
        logger.info(f"{len(bz2_files)}개의 .bz2 파일을 발견했습니다.")

        for file_path in bz2_files:
            size_mb = file_path.stat().st_size / (1024 * 1024)
            logger.info(f"  - {file_path.name}: {size_mb:.2f} MB")

        return bz2_files

    def create_progress_callback(self, filename: str):
        """
        진행률 표시 콜백 함수 생성

        Args:
            filename: 업로드 중인 파일명

        Returns:
            콜백 함수
        """
        last_print_time = [0]  # mutable 객체로 사용

        def progress_callback(progress: UploadProgress):
            current_time = time.time()
            # 1초마다 또는 100% 완료 시 진행률 출력
            if (
                current_time - last_print_time[0] > 1.0
                or progress.progress_percent >= 100
            ):
                print(
                    f"\r업로드 진행률 [{filename}]: {progress.progress_percent:.1f}% "
                    f"({progress.bytes_transferred:,}/{progress.total_bytes:,} bytes)",
                    end="",
                    flush=True,
                )
                last_print_time[0] = current_time

                if progress.progress_percent >= 100:
                    print()  # 줄바꿈

        return progress_callback

    def upload_file_with_retry(
        self, file_path: Path, max_retries: Optional[int] = None
    ) -> bool:
        """
        재시도 로직을 포함한 파일 업로드

        Args:
            file_path: 업로드할 파일 경로
            max_retries: 최대 재시도 횟수 (None인 경우 설정에서 읽음)

        Returns:
            업로드 성공 시 True, 실패 시 False
        """
        if max_retries is None:
            max_retries = settings.max_retries
        retry_delay = settings.retry_delay

        object_key = f"raw/{file_path.name}"

        # 이미 업로드된 파일인지 확인
        if self.minio_service.object_exists(self.bucket_name, object_key):
            logger.info(f"파일이 이미 존재합니다. 스킵: {object_key}")
            return True

        # 메타데이터 준비
        extra_args = {
            "Metadata": {
                "source": "bigdata",
                "upload-date": datetime.now().isoformat(),
                "original-name": file_path.name,
                "file-type": "wiki-dump",
            },
            "ContentType": "application/x-bzip2",
        }

        progress_callback = self.create_progress_callback(file_path.name)

        for attempt in range(max_retries):
            try:
                logger.info(
                    f"업로드 시도 {attempt + 1}/{max_retries}: {file_path.name}"
                )

                success = self.minio_service.upload_file(
                    str(file_path),
                    self.bucket_name,
                    object_key,
                    extra_args=extra_args,
                    progress_callback=progress_callback,
                )

                if success:
                    # 업로드 검증
                    obj_info = self.minio_service.get_object_info(
                        self.bucket_name, object_key
                    )
                    if obj_info:
                        local_size = file_path.stat().st_size
                        remote_size = obj_info["size"]

                        if local_size == remote_size:
                            logger.info(f"✅ 업로드 성공 및 검증 완료: {object_key}")
                            return True
                        else:
                            logger.error(
                                f"❌ 파일 크기 불일치: 로컬({local_size}) vs 원격({remote_size})"
                            )
                    else:
                        logger.error("❌ 업로드 검증 실패: 객체 정보를 가져올 수 없음")

                logger.warning(
                    f"업로드 실패. 재시도합니다... ({attempt + 1}/{max_retries})"
                )
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)

            except Exception as e:
                logger.error(f"업로드 중 오류 발생: {e}")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                else:
                    logger.error(
                        f"❌ 최대 재시도 횟수 초과. 업로드 실패: {file_path.name}"
                    )

        return False

    def upload_all_files(self) -> Dict[str, Any]:
        """
        모든 .bz2 파일을 업로드

        Returns:
            업로드 결과 딕셔너리 (성공/실패 카운트)
        """
        bz2_files = self.find_bz2_files()

        if not bz2_files:
            logger.warning("업로드할 .bz2 파일을 찾을 수 없습니다.")
            return {"success": 0, "failed": 0, "skipped": 0}

        results = {"success": 0, "failed": 0, "skipped": 0}
        total_files = len(bz2_files)

        logger.info(f"총 {total_files}개의 파일 업로드를 시작합니다.")
        print("=" * 60)

        for i, file_path in enumerate(bz2_files, 1):
            print(f"\n[{i}/{total_files}] {file_path.name} 업로드 중...")

            start_time = time.time()

            if self.upload_file_with_retry(file_path):
                results["success"] += 1
                elapsed_time = time.time() - start_time
                logger.info(f"✅ 업로드 완료: {file_path.name} ({elapsed_time:.2f}초)")
            else:
                results["failed"] += 1
                logger.error(f"❌ 업로드 실패: {file_path.name}")

        print("\n" + "=" * 60)
        logger.info(
            f"업로드 완료 - 성공: {results['success']}, 실패: {results['failed']}"
        )

        return results


