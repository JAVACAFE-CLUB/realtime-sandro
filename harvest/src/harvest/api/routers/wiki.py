"""위키 데이터 수집 API 엔드포인트"""

from datetime import datetime

from fastapi import APIRouter, BackgroundTasks, HTTPException, status

from ...services.wiki_service import WikiService
from ...schemas.wiki import (
    UploadState, UploadStatus, UploadResponse, 
    FilesResponse, BucketsResponse, BucketCreateResponse
)

router = APIRouter()

# 전역 상태 저장 (향후 Redis로 교체 가능)
upload_state = UploadState()

# 서비스 인스턴스
wiki_service = WikiService()


async def upload_wiki_files_task():
    """백그라운드에서 위키 파일 업로드를 수행하는 태스크"""
    global upload_state
    
    try:
        upload_state.status = UploadStatus.PROCESSING
        upload_state.started_at = datetime.now()
        upload_state.error_message = None
        
        # 서비스를 통해 업로드 수행
        results = wiki_service.upload_all_files()
        
        # 결과를 상태에 반영
        upload_state.completed = results.get("success", [])
        upload_state.failed = results.get("failed", [])
        upload_state.total_files = results.get("success", 0) + results.get("failed", 0)
        upload_state.progress = 100.0
        upload_state.status = UploadStatus.COMPLETED
        upload_state.completed_at = datetime.now()
        upload_state.current_file = None
        
    except Exception as e:
        upload_state.status = UploadStatus.FAILED
        upload_state.error_message = str(e)
        upload_state.completed_at = datetime.now()


@router.post("/upload", response_model=UploadResponse)
async def trigger_wiki_upload(background_tasks: BackgroundTasks):
    """위키 파일 업로드를 백그라운드로 시작"""
    global upload_state
    
    # 이미 처리 중인 경우 에러 반환
    if upload_state.status == UploadStatus.PROCESSING:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Upload is already in progress"
        )
    
    # 상태 초기화
    upload_state = UploadState()
    
    # 백그라운드 태스크 추가
    background_tasks.add_task(upload_wiki_files_task)
    
    return UploadResponse(
        message="Wiki upload started",
        status=upload_state.status,
        task_id=datetime.now().isoformat()
    )


@router.get("/status")
async def get_upload_status() -> UploadState:
    """현재 업로드 상태 조회"""
    return upload_state


@router.get("/files", response_model=FilesResponse)
async def list_available_files():
    """업로드 가능한 위키 파일 목록 조회"""
    try:
        return FilesResponse(**wiki_service.get_available_files())
    except FileNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@router.post("/upload/{filename}", response_model=UploadResponse)
async def upload_specific_file(
    filename: str,
    background_tasks: BackgroundTasks
):
    """특정 위키 파일 업로드"""
    global upload_state
    
    if upload_state.status == UploadStatus.PROCESSING:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Another upload is in progress"
        )
    
    # 상태 초기화
    upload_state.__init__()
    
    # 단일 파일 업로드 태스크
    async def upload_single_file():
        global upload_state
        try:
            upload_state.status = UploadStatus.PROCESSING
            upload_state.started_at = datetime.now()
            upload_state.current_file = filename
            upload_state.total_files = 1
            
            success = wiki_service.upload_single_file(filename)
            
            if success:
                upload_state.completed.append(filename)
                upload_state.status = UploadStatus.COMPLETED
            else:
                upload_state.failed.append(filename)
                upload_state.status = UploadStatus.FAILED
            
            upload_state.progress = 100.0
            upload_state.completed_at = datetime.now()
            upload_state.current_file = None
            
        except Exception as e:
            upload_state.status = UploadStatus.FAILED
            upload_state.error_message = str(e)
            upload_state.completed_at = datetime.now()
    
    background_tasks.add_task(upload_single_file)
    
    return UploadResponse(
        message=f"Upload started for {filename}",
        status=upload_state.status,
        filename=filename
    )


@router.get("/buckets", response_model=BucketsResponse)
async def list_buckets():
    """MinIO 버킷 목록 조회"""
    try:
        bucket_list = wiki_service.get_buckets()
        return BucketsResponse(
            total=len(bucket_list),
            buckets=bucket_list
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@router.post("/buckets/{bucket_name}", response_model=BucketCreateResponse)
async def create_bucket(bucket_name: str):
    """새 버킷 생성"""
    try:
        success = wiki_service.create_bucket(bucket_name)
        
        if success:
            return BucketCreateResponse(
                message=f"Bucket '{bucket_name}' created or already exists",
                bucket_name=bucket_name
            )
        else:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to create bucket: {bucket_name}"
            )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )