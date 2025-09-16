from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional, Any

from pydantic import BaseModel, Field


class DocumentType(str, Enum):
    """문서 타입"""
    WIKI = "WIKI"      # 위키페이지
    NEWS = "NEWS"      # 뉴스 HTML
    TWEET = "TWEET"    # X API 응답


class CreateDocumentRequest(BaseModel):
    """문서 생성 요청 DTO"""
    
    document_type: DocumentType = Field(..., description="문서 타입은 필수입니다")
    
    title: Optional[str] = Field(None, max_length=500, description="제목은 500자를 초과할 수 없습니다")
    
    content: str = Field(..., min_length=1, description="내용은 필수입니다")
    
    source: Optional[str] = Field(None, description="출처")
    
    published_at: Optional[datetime] = Field(None, description="발행일시")
    
    author: Optional[str] = Field(None, description="작성자")
    
    categories: List[str] = Field(default_factory=list, description="카테고리 목록")
    
    tags: List[str] = Field(default_factory=list, description="태그 목록")
    
    url: Optional[str] = Field(None, description="원본 URL")
    
    metadata: Dict[str, Any] = Field(default_factory=dict, description="메타데이터")
    
    class Config:
        # camelCase를 snake_case로 변환하여 받을 수 있도록 설정
        alias_generator = lambda field_name: field_name
        populate_by_name = True