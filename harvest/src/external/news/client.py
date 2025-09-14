import logging
from typing import Optional
from urllib.parse import urlencode

import feedparser
import requests
from feedparser import FeedParserDict

from .categories import NewsCategory
from .collection import NewsArticleCollection

logger = logging.getLogger(__name__)


def get_feed_with_requests(url: str) -> FeedParserDict:
    """
    requests를 사용하여 RSS 피드를 안전하게 가져오기
    SSL 인증서 문제와 봇 차단을 방지하기 위해 User-Agent 헤더 추가
    """
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }

    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        return feedparser.parse(response.content)
    except requests.exceptions.RequestException as e:
        logger.error(f"Failed to fetch feed from {url}: {e}")
        # 기본 feedparser로 재시도
        return feedparser.parse(url)


class GoogleNewsClient:
    """구글 뉴스 RSS 피드 클라이언트"""

    def __init__(self):
        self.base_url = "https://news.google.com/rss"
        self.search_url = "https://news.google.com/rss/search"

    def get_top_articles(self, language: str = 'ko', country: str = 'KR') -> NewsArticleCollection:
        """
        톱 뉴스 기사를 바로 타입 안전한 객체로 반환

        Args:
            language: 언어 코드 (예: 'ko', 'en')
            country: 국가 코드 (예: 'KR', 'US')

        Returns:
            NewsArticleCollection: 검증된 뉴스 기사 컬렉션
        """
        url = self._build_url(hl=language, gl=country, ceid=f"{country}:{language}")
        logger.info(f"Fetching top news from: {url}")
        feed_data = get_feed_with_requests(url)
        return NewsArticleCollection.from_feed_data(feed_data)

    def search_articles(self, query: str, when: Optional[str] = None,
                        language: str = 'ko', country: str = 'KR') -> NewsArticleCollection:
        """
        키워드로 뉴스 검색하여 바로 타입 안전한 객체로 반환

        Args:
            query: 검색 키워드
            when: 시간 필터 (예: '7d', '30d')
            language: 언어 코드
            country: 국가 코드

        Returns:
            NewsArticleCollection: 검증된 뉴스 기사 컬렉션
        """
        url = self._build_search_url(country, language, query, when)
        logger.info(f"Searching news with query '{query}' from: {url}")
        feed_data = get_feed_with_requests(url)
        return NewsArticleCollection.from_feed_data(feed_data)

    def get_category_articles(self, category: NewsCategory, language: str = 'ko', country: str = 'KR') -> NewsArticleCollection:
        """
        카테고리별 뉴스를 바로 타입 안전한 객체로 반환

        Args:
            category: NewsCategory Enum
            language: 언어 코드
            country: 국가 코드

        Returns:
            NewsArticleCollection: 검증된 뉴스 기사 컬렉션
        """
        query = category.get_query_for_language(language)
        return self.search_articles(query, language=language, country=country)

    def _build_url(self, **params) -> str:
        """
        URL 생성 헬퍼 메서드

        Args:
            **params: URL 파라미터들

        Returns:
            str: 완성된 URL
        """
        if params:
            return f"{self.base_url}?{urlencode(params)}"
        return self.base_url

    def _build_search_url(self, country: str, language: str, query: str, when: str | None) -> str:
        search_query = query
        if when:
            search_query = f"{query}+when:{when}"

        params = {
            'q': search_query,
            'hl': language,
            'gl': country,
            'ceid': f"{country}:{language}"
        }

        if params:
            return f"{self.search_url}?{urlencode(params)}"
        return self.search_url
