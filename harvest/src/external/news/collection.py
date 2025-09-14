import logging
from typing import List, Iterator

from feedparser import FeedParserDict
from pydantic import ValidationError

from ...schemas.news import NewsArticle

logger = logging.getLogger(__name__)


class NewsArticleCollection:
    """뉴스 기사 리스트를 래핑하는 컬렉션 클래스"""

    def __init__(self, articles: List[NewsArticle]):
        """
        뉴스 기사 컬렉션 초기화
        
        Args:
            articles: NewsArticle 객체들의 리스트
        """
        self._articles = articles

    def __len__(self) -> int:
        """컬렉션의 길이 반환"""
        return len(self._articles)

    def __iter__(self) -> Iterator[NewsArticle]:
        """컬렉션 순회를 위한 이터레이터 반환"""
        return iter(self._articles)

    def __getitem__(self, index) -> NewsArticle:
        """인덱스를 통한 기사 접근"""
        return self._articles[index]

    def __bool__(self) -> bool:
        """컬렉션이 비어있지 않은지 확인"""
        return bool(self._articles)

    def append(self, article: NewsArticle) -> None:
        """새로운 기사 추가"""
        self._articles.append(article)

    def extend(self, articles: List[NewsArticle]) -> None:
        """여러 기사들을 한 번에 추가"""
        self._articles.extend(articles)

    def to_list(self) -> List[NewsArticle]:
        """내부 리스트 반환 (호환성을 위해)"""
        return self._articles.copy()

    def to_dict_list(self) -> List[dict]:
        """딕셔너리 리스트로 변환"""
        return [article.to_dict() for article in self._articles]

    def filter_by_source(self, source: str) -> 'NewsArticleCollection':
        """특정 소스의 기사들만 필터링"""
        filtered_articles = [article for article in self._articles if article.source == source]
        return NewsArticleCollection(filtered_articles)

    def filter_by_keyword(self, keyword: str) -> 'NewsArticleCollection':
        """제목이나 설명에 키워드가 포함된 기사들만 필터링"""
        keyword_lower = keyword.lower()
        filtered_articles = [
            article for article in self._articles
            if keyword_lower in article.title.lower() or
               (article.description and keyword_lower in article.description.lower())
        ]
        return NewsArticleCollection(filtered_articles)

    @classmethod
    def from_feed_data(cls, feed_data: FeedParserDict) -> 'NewsArticleCollection':
        """
        RSS 피드 데이터로부터 NewsArticleCollection을 생성하는 팩터리 메서드
        
        Args:
            feed_data: 파싱된 RSS 피드 데이터
            
        Returns:
            NewsArticleCollection: 검증된 뉴스 기사 컬렉션
        """
        articles = []

        for entry in feed_data.get('entries', []):
            try:
                # 안전하게 source 정보 추출
                source_title = ""
                if 'source' in entry and isinstance(entry['source'], dict):
                    source_title = entry['source'].get('title', '')

                # NewsArticle 모델로 데이터 검증 및 변환
                article = NewsArticle(
                    title=entry.get('title', ''),
                    link=entry.get('link', ''),
                    description=entry.get('description', ''),
                    published=entry.get('published', ''),
                    published_date=entry.get('published_parsed'),  # 자동으로 datetime으로 변환됨
                    id=entry.get('id', ''),
                    source=source_title
                )
                articles.append(article)

            except ValidationError as e:
                # 검증 실패한 기사는 로그 남기고 건너뛰기
                logger.warning(f"기사 데이터 검증 실패, 건너뜀: {e}")
                continue
            except Exception as e:
                # 예상치 못한 에러도 로그 남기고 건너뛰기
                logger.error(f"기사 추출 중 오류 발생: {e}")
                continue

        return cls(articles)
