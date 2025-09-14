from enum import Enum
from typing import Dict


class NewsCategory(Enum):
    """뉴스 카테고리 Enum 클래스"""

    WORLD = "WORLD"
    NATION = "NATION"
    BUSINESS = "BUSINESS"
    TECHNOLOGY = "TECHNOLOGY"
    ENTERTAINMENT = "ENTERTAINMENT"
    SPORTS = "SPORTS"
    SCIENCE = "SCIENCE"
    HEALTH = "HEALTH"

    @property
    def korean_query(self) -> str:
        """한국어 검색어 반환"""
        return self._korean_queries()[self]

    @property
    def english_query(self) -> str:
        """영어 검색어 반환"""
        return self._english_queries()[self]

    def get_query_for_language(self, language: str = 'ko') -> str:
        """
        언어별 검색어 반환
        
        Args:
            language: 언어 코드 ('ko' 또는 'en')
            
        Returns:
            str: 해당 언어의 검색어
        """
        if language == 'ko':
            return self.korean_query
        elif language == 'en':
            return self.english_query
        else:
            # 지원하지 않는 언어인 경우 소문자 영어 반환
            return self.value.lower()

    @classmethod
    def _korean_queries(cls) -> Dict['NewsCategory', str]:
        """한국어 검색어 매핑"""
        return {
            cls.WORLD: '세계',
            cls.NATION: '국내',
            cls.BUSINESS: '경제',
            cls.TECHNOLOGY: '기술',
            cls.ENTERTAINMENT: '연예',
            cls.SPORTS: '스포츠',
            cls.SCIENCE: '과학',
            cls.HEALTH: '건강'
        }

    @classmethod
    def _english_queries(cls) -> Dict['NewsCategory', str]:
        """영어 검색어 매핑"""
        return {
            cls.WORLD: 'world',
            cls.NATION: 'domestic',
            cls.BUSINESS: 'business',
            cls.TECHNOLOGY: 'technology',
            cls.ENTERTAINMENT: 'entertainment',
            cls.SPORTS: 'sports',
            cls.SCIENCE: 'science',
            cls.HEALTH: 'health'
        }

    @classmethod
    def from_string(cls, category_str: str) -> 'NewsCategory':
        """
        문자열로부터 NewsCategory 생성
        
        Args:
            category_str: 카테고리 문자열
            
        Returns:
            NewsCategory: 해당하는 카테고리 Enum
            
        Raises:
            ValueError: 지원하지 않는 카테고리인 경우
        """
        try:
            return cls(category_str.upper())
        except ValueError:
            # 지원하는 카테고리 목록
            supported = [category.value for category in cls]
            raise ValueError(
                f"지원하지 않는 카테고리입니다: '{category_str}'. "
                f"지원 카테고리: {supported}"
            )

    @classmethod
    def get_all_categories(cls) -> Dict[str, str]:
        """
        모든 카테고리와 한국어 검색어 매핑 반환
        (기존 코드 호환성을 위한 메서드)
        """
        return {category.value: category.korean_query for category in cls}

    def __str__(self) -> str:
        return self.value
