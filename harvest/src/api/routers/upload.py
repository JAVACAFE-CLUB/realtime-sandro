"""업로드 관련 API 엔드포인트"""

import logging
from datetime import datetime
from typing import Dict, Any, List, Optional
from pathlib import Path

from fastapi import APIRouter, HTTPException, status, BackgroundTasks, Depends
from fastapi.responses import JSONResponse

from ...core.config import settings
from ...services.upload_service import UploadService
from ...schemas.upload import UploadResponse, UploadResult, FileListResponse

router = APIRouter()
logger = logging.getLogger(__name__)


def get_upload_service() -> UploadService:
    """UploadService 의존성 주입"""
    try:
        return UploadService(
            data_dir=settings.wiki_data_dir,
            bucket_name=settings.wiki_bucket_name
        )
    except Exception as e:
        logger.error(f"UploadService 초기화 실패: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"서비스 초기화 실패: {str(e)}"
        )


@router.get("/files", response_model=FileListResponse)
async def list_upload_files(
    upload_service: UploadService = Depends(get_upload_service)
) -> FileListResponse:
    """업로드 가능한 .bz2 파일 목록 조회"""
    try:
        bz2_files = upload_service.find_bz2_files()
        
        files_info = []
        for file_path in bz2_files:
            file_stat = file_path.stat()
            files_info.append({
                "filename": file_path.name,
                "filepath": str(file_path),
                "size": file_stat.st_size,
                "size_mb": round(file_stat.st_size / (1024 * 1024), 2),
                "modified_at": datetime.fromtimestamp(file_stat.st_mtime).isoformat()
            })
        
        return FileListResponse(
            status="success",
            message=f"{len(files_info)}개의 .bz2 파일을 찾았습니다.",
            files=files_info,
            count=len(files_info)
        )
        
    except Exception as e:
        logger.error(f"파일 목록 조회 실패: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"파일 목록 조회 실패: {str(e)}"
        )


@router.post("/all", response_model=UploadResponse)
async def upload_all_files(
    upload_service: UploadService = Depends(get_upload_service)
) -> UploadResponse:
    """모든 .bz2 파일을 동기적으로 업로드"""
    start_time = datetime.now()
    
    try:
        logger.info("전체 파일 업로드 시작")
        
        # 업로드 가능한 파일 개수 확인
        bz2_files = upload_service.find_bz2_files()
        if not bz2_files:
            return UploadResponse(
                status="success",
                message="업로드할 .bz2 파일이 없습니다.",
                result=UploadResult(
                    success=0,
                    failed=0,
                    skipped=0,
                    total=0,
                    started_at=start_time,
                    completed_at=datetime.now(),
                    duration_seconds=0.0
                )
            )
        
        # 업로드 실행
        upload_results = upload_service.upload_all_files()
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        total_files = upload_results["success"] + upload_results["failed"] + upload_results["skipped"]
        
        result = UploadResult(
            success=upload_results["success"],
            failed=upload_results["failed"],
            skipped=upload_results["skipped"],
            total=total_files,
            started_at=start_time,
            completed_at=end_time,
            duration_seconds=round(duration, 2)
        )
        
        if upload_results["failed"] > 0:
            message = f"업로드 완료 - 성공: {result.success}, 실패: {result.failed}, 스킵: {result.skipped}"
            logger.warning(f"{message} (소요시간: {duration:.2f}초)")
        else:
            message = f"모든 파일 업로드 성공 - 성공: {result.success}, 스킵: {result.skipped}"
            logger.info(f"{message} (소요시간: {duration:.2f}초)")
        
        return UploadResponse(
            status="success",
            message=message,
            result=result
        )
        
    except Exception as e:
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.error(f"전체 파일 업로드 실패: {e}")
        
        return UploadResponse(
            status="error",
            message="파일 업로드 중 오류가 발생했습니다.",
            result=UploadResult(
                success=0,
                failed=0,
                skipped=0,
                total=0,
                started_at=start_time,
                completed_at=end_time,
                duration_seconds=round(duration, 2)
            ),
            error_details={
                "error_type": type(e).__name__,
                "error_message": str(e)
            }
        )


async def _background_upload_all(upload_service: UploadService) -> Dict[str, Any]:
    """백그라운드에서 모든 파일 업로드 실행"""
    start_time = datetime.now()
    
    try:
        logger.info("백그라운드 전체 파일 업로드 시작")
        
        upload_results = upload_service.upload_all_files()
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info(f"백그라운드 업로드 완료 - 소요시간: {duration:.2f}초")
        logger.info(f"결과: 성공={upload_results['success']}, 실패={upload_results['failed']}, 스킵={upload_results['skipped']}")
        
        return upload_results
        
    except Exception as e:
        logger.error(f"백그라운드 업로드 실패: {e}")
        raise


@router.post("/all/background")
async def upload_all_files_background(
    background_tasks: BackgroundTasks,
    upload_service: UploadService = Depends(get_upload_service)
) -> JSONResponse:
    """모든 .bz2 파일을 백그라운드에서 비동기적으로 업로드"""
    try:
        # 업로드 가능한 파일 개수 확인
        bz2_files = upload_service.find_bz2_files()
        if not bz2_files:
            return JSONResponse(
                status_code=status.HTTP_200_OK,
                content={
                    "status": "success",
                    "message": "업로드할 .bz2 파일이 없습니다.",
                    "files_count": 0
                }
            )
        
        # 백그라운드 작업 추가
        background_tasks.add_task(_background_upload_all, upload_service)
        
        return JSONResponse(
            status_code=status.HTTP_202_ACCEPTED,
            content={
                "status": "accepted",
                "message": f"{len(bz2_files)}개 파일의 백그라운드 업로드가 시작되었습니다.",
                "files_count": len(bz2_files),
                "note": "업로드 진행 상황은 서버 로그를 확인하세요."
            }
        )
        
    except Exception as e:
        logger.error(f"백그라운드 업로드 시작 실패: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"백그라운드 업로드 시작 실패: {str(e)}"
        )


@router.post("/file/{filename}")
async def upload_single_file(
    filename: str,
    upload_service: UploadService = Depends(get_upload_service)
) -> UploadResponse:
    """특정 파일 하나만 업로드"""
    start_time = datetime.now()
    
    try:
        # 파일 존재 확인
        file_path = Path(settings.wiki_data_dir) / filename
        if not file_path.exists():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"파일을 찾을 수 없습니다: {filename}"
            )
        
        if not filename.endswith('.bz2'):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="지원하는 파일 형식이 아닙니다. .bz2 파일만 업로드 가능합니다."
            )
        
        logger.info(f"단일 파일 업로드 시작: {filename}")
        
        # 업로드 실행
        success = upload_service.upload_file_with_retry(file_path)
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        result = UploadResult(
            success=1 if success else 0,
            failed=0 if success else 1,
            skipped=0,
            total=1,
            started_at=start_time,
            completed_at=end_time,
            duration_seconds=round(duration, 2)
        )
        
        if success:
            message = f"파일 업로드 성공: {filename}"
            logger.info(f"{message} (소요시간: {duration:.2f}초)")
        else:
            message = f"파일 업로드 실패: {filename}"
            logger.error(f"{message} (소요시간: {duration:.2f}초)")
        
        return UploadResponse(
            status="success" if success else "error",
            message=message,
            result=result
        )
        
    except HTTPException:
        raise
    except Exception as e:
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.error(f"단일 파일 업로드 실패: {e}")
        
        return UploadResponse(
            status="error",
            message=f"파일 업로드 실패: {filename}",
            result=UploadResult(
                success=0,
                failed=1,
                skipped=0,
                total=1,
                started_at=start_time,
                completed_at=end_time,
                duration_seconds=round(duration, 2)
            ),
            error_details={
                "error_type": type(e).__name__,
                "error_message": str(e),
                "filename": filename
            }
        )