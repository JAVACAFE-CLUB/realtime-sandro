"""업로드 관련 API 응답 스키마"""

from datetime import datetime
from typing import Dict, Any, List, Optional

from pydantic import BaseModel, Field


class UploadResult(BaseModel):
    """파일 업로드 결과 스키마"""
    success: int = Field(..., description="성공적으로 업로드된 파일 수")
    failed: int = Field(..., description="업로드 실패한 파일 수")
    skipped: int = Field(..., description="이미 존재하여 스킵된 파일 수")
    total: int = Field(..., description="전체 처리된 파일 수")
    started_at: datetime = Field(..., description="업로드 시작 시간")
    completed_at: Optional[datetime] = Field(None, description="업로드 완료 시간")
    duration_seconds: Optional[float] = Field(None, description="소요 시간 (초)")


class UploadResponse(BaseModel):
    """업로드 API 응답 스키마"""
    status: str = Field(..., description="응답 상태 (success/error)")
    message: str = Field(..., description="응답 메시지")
    result: Optional[UploadResult] = Field(None, description="업로드 결과 상세")
    error_details: Optional[Dict[str, Any]] = Field(None, description="오류 상세 정보")


class FileListResponse(BaseModel):
    """파일 목록 응답 스키마"""
    status: str = Field(..., description="응답 상태")
    message: str = Field(..., description="응답 메시지")
    files: List[Dict[str, Any]] = Field(..., description="파일 목록")
    count: int = Field(..., description="파일 개수")