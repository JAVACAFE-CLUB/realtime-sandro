"""X API 스키마 정의"""
from datetime import datetime
from typing import Optional, List, Dict, Any, Union

from pydantic import BaseModel, Field, field_validator


class TweetSearchRequest(BaseModel):
    """트윗 검색 요청"""
    query: str = Field(..., description="검색 쿼리")
    max_results: int = Field(default=10, ge=1, le=100, description="최대 결과 수")
    start_time: Optional[datetime] = Field(default=None, description="검색 시작 시간")
    end_time: Optional[datetime] = Field(default=None, description="검색 종료 시간")


class UserTimelineRequest(BaseModel):
    """사용자 타임라인 요청"""
    username: str = Field(..., description="사용자명 (@ 제외)")
    max_results: int = Field(default=10, ge=1, le=100, description="최대 결과 수")
    exclude_replies: bool = Field(default=True, description="답글 제외")
    exclude_retweets: bool = Field(default=True, description="리트윗 제외")


class TweetResponse(BaseModel):
    """트윗 응답"""
    id: str
    text: str
    created_at: Optional[str] = None
    author_id: str
    username: Optional[str] = None
    lang: Optional[str] = None
    metrics: Optional[Dict[str, Any]] = None
    
    @field_validator('id', 'author_id', mode='before')
    @classmethod
    def convert_id_to_string(cls, v: Union[str, int]) -> str:
        """ID 값을 문자열로 변환"""
        return str(v)


class TweetListResponse(BaseModel):
    """트윗 목록 응답"""
    tweets: List[TweetResponse]
    count: int
    query: Optional[str] = None
