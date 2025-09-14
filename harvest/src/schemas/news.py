from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class NewsArticle(BaseModel):
    """뉴스 기사 정보를 담는 Pydantic 모델"""

    title: str = Field(description="뉴스 제목")
    link: str = Field(description="뉴스 링크 URL")
    description: Optional[str] = Field(default="", description="뉴스 설명/요약")
    published: Optional[str] = Field(default="", description="발행일 문자열 (RFC 2822 형식)")
    published_date: Optional[datetime] = Field(default=None, description="파싱된 발행일 (datetime 객체)")
    id: Optional[str] = Field(default="", description="뉴스 고유 ID")
    source: Optional[str] = Field(default="", description="뉴스 소스/출처")

    @field_validator('published_date', mode='before')
    @classmethod
    def parse_published_date(cls, v) -> Optional[datetime]:
        """
        time.struct_time 또는 튜플을 datetime으로 변환
        
        Args:
            v: time.struct_time, 튜플, datetime, 또는 None
            
        Returns:
            Optional[datetime]: 변환된 datetime 객체 또는 None
        """
        if v is None:
            return None

        if isinstance(v, datetime):
            return v

        # time.struct_time 또는 9개 요소를 가진 튜플인 경우
        if hasattr(v, 'tm_year') or (isinstance(v, (tuple, list)) and len(v) >= 6):
            try:
                if hasattr(v, 'tm_year'):
                    # time.struct_time 객체
                    return datetime(*v[:6])
                else:
                    # 튜플 또는 리스트
                    return datetime(*v[:6])
            except (ValueError, TypeError, OverflowError):
                return None

        return None

    @field_validator('link')
    @classmethod
    def validate_link(cls, v: str) -> str:
        """링크가 비어있지 않고 유효한 형식인지 확인"""
        if not v or not v.strip():
            raise ValueError("링크는 비어있을 수 없습니다")
        return v.strip()

    @field_validator('title')
    @classmethod
    def validate_title(cls, v: str) -> str:
        """제목이 비어있지 않은지 확인"""
        if not v or not v.strip():
            raise ValueError("제목은 비어있을 수 없습니다")
        return v.strip()

    def to_dict(self) -> dict:
        """
        호환성을 위한 딕셔너리 변환 메서드
        기존 코드에서 딕셔너리로 접근하는 부분을 위해 제공
        """
        return {
            'title': self.title,
            'link': self.link,
            'description': self.description,
            'published': self.published,
            'published_parsed': self.published_date,
            'id': self.id,
            'source': self.source
        }

    model_config = {
        "json_encoders": {
            datetime: lambda v: v.isoformat() if v else None
        }
    }
