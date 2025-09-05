"""위키 API 스키마 정의"""

from datetime import datetime
from typing import List, Optional
from enum import Enum

from pydantic import BaseModel, Field


class UploadStatus(str, Enum):
    """업로드 상태 열거형"""
    IDLE = "idle"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class FileInfo(BaseModel):
    """파일 정보 모델"""
    name: str = Field(..., description="파일명")
    size_mb: float = Field(..., ge=0, description="파일 크기 (MB)")
    path: str = Field(..., description="파일 경로")


class FilesResponse(BaseModel):
    """파일 목록 응답 모델"""
    total: int = Field(..., ge=0, description="총 파일 개수")
    files: List[FileInfo] = Field(..., description="파일 목록")
    directory: str = Field(..., description="디렉토리 경로")


class UploadState(BaseModel):
    """업로드 상태 모델"""
    status: UploadStatus = Field(default=UploadStatus.IDLE, description="업로드 상태")
    current_file: Optional[str] = Field(None, description="현재 처리 중인 파일")
    progress: float = Field(default=0.0, ge=0.0, le=100.0, description="진행률 (%)")
    completed: List[str] = Field(default_factory=list, description="완료된 파일 목록")
    failed: List[str] = Field(default_factory=list, description="실패한 파일 목록")
    total_files: int = Field(default=0, ge=0, description="총 파일 개수")
    started_at: Optional[datetime] = Field(None, description="시작 시각")
    completed_at: Optional[datetime] = Field(None, description="완료 시각")
    error_message: Optional[str] = Field(None, description="오류 메시지")


class UploadResponse(BaseModel):
    """업로드 시작 응답 모델"""
    message: str = Field(..., description="응답 메시지")
    status: UploadStatus = Field(..., description="업로드 상태")
    task_id: Optional[str] = Field(None, description="작업 ID")
    filename: Optional[str] = Field(None, description="파일명 (단일 파일 업로드 시)")


class BucketInfo(BaseModel):
    """버킷 정보 모델"""
    name: str = Field(..., description="버킷명")
    creation_date: str = Field(..., description="생성일")
    object_count: int = Field(..., ge=0, description="객체 개수")


class BucketsResponse(BaseModel):
    """버킷 목록 응답 모델"""
    total: int = Field(..., ge=0, description="총 버킷 개수")
    buckets: List[BucketInfo] = Field(..., description="버킷 목록")


class BucketCreateResponse(BaseModel):
    """버킷 생성 응답 모델"""
    message: str = Field(..., description="응답 메시지")
    bucket_name: str = Field(..., description="생성된 버킷명")