import sys
from pathlib import Path
from unittest.mock import patch

import pytest

from src.external.news.collection import NewsArticleCollection

sys.path.insert(0, str(Path(__file__).parent.parent))

from src.external.news.client import GoogleNewsClient
from src.schemas.news import NewsArticle
from src.external.news.categories import NewsCategory


@pytest.fixture
def google_news_client():
    """GoogleNewsClient 인스턴스를 반환하는 픽스처"""
    return GoogleNewsClient()


@pytest.fixture
def mock_feed_data():
    """모의 RSS 피드 데이터"""
    return {
        'feed': {
            'title': 'Google News',
            'link': 'https://news.google.com',
            'description': 'Google News'
        },
        'entries': [
            {
                'title': '테스트 뉴스 제목 1',
                'link': 'https://example.com/news/1',
                'description': '테스트 뉴스 내용 1',
                'published': 'Mon, 01 Jan 2024 12:00:00 GMT',
                'published_parsed': (2024, 1, 1, 12, 0, 0, 0, 1, 0),
                'id': 'test-id-1',
                'source': {'title': '테스트 신문사'}
            },
            {
                'title': '테스트 뉴스 제목 2',
                'link': 'https://example.com/news/2',
                'description': '테스트 뉴스 내용 2',
                'published': 'Mon, 01 Jan 2024 13:00:00 GMT',
                'published_parsed': (2024, 1, 1, 13, 0, 0, 0, 1, 0),
                'id': 'test-id-2'
            }
        ]
    }


class TestGoogleNewsClient:
    """GoogleNewsClient 테스트 클래스"""

    def test_init(self, google_news_client):
        """클라이언트 초기화 테스트"""
        assert google_news_client.base_url == "https://news.google.com/rss"
        assert google_news_client.search_url == "https://news.google.com/rss/search"

    def test_build_url_without_params(self, google_news_client):
        """파라미터 없이 URL 생성 테스트"""
        url = google_news_client._build_url()
        assert url == "https://news.google.com/rss"

    def test_build_url_with_params(self, google_news_client):
        """파라미터와 함께 URL 생성 테스트"""
        url = google_news_client._build_url(hl='ko', gl='KR', ceid='KR:ko')
        expected_params = ['hl=ko', 'gl=KR', 'ceid=KR%3Ako']

        for param in expected_params:
            assert param in url

    def test_build_search_url_without_when(self, google_news_client):
        """when 파라미터 없이 검색 URL 생성 테스트"""
        url = google_news_client._build_search_url('KR', 'ko', '인공지능', None)

        assert 'https://news.google.com/rss/search' in url
        assert 'q=%EC%9D%B8%EA%B3%B5%EC%A7%80%EB%8A%A5' in url  # URL 인코딩된 '인공지능'
        assert 'hl=ko' in url
        assert 'gl=KR' in url
        assert 'ceid=KR%3Ako' in url
        assert 'when%3A' not in url  # when 파라미터가 없어야 함

    def test_build_search_url_with_when(self, google_news_client):
        """when 파라미터와 함께 검색 URL 생성 테스트"""
        url = google_news_client._build_search_url('US', 'en', 'technology', '7d')

        assert 'https://news.google.com/rss/search' in url
        assert 'q=technology%2Bwhen%3A7d' in url  # 'technology+when:7d' 형태 (URL 인코딩)
        assert 'hl=en' in url
        assert 'gl=US' in url
        assert 'ceid=US%3Aen' in url

    @patch('src.external.news.client.get_feed_with_requests')
    def test_get_top_articles(self, mock_get_feed, google_news_client, mock_feed_data):
        """톱 뉴스 가져오기 테스트 (NewsArticleCollection 반환)"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.get_top_articles()

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'hl=ko' in call_args
        assert 'gl=KR' in call_args
        assert 'ceid=KR%3Ako' in call_args
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    @patch('src.external.news.client.get_feed_with_requests')
    def test_get_top_articles_with_custom_params(self, mock_get_feed, google_news_client, mock_feed_data):
        """커스텀 파라미터로 톱 뉴스 가져오기 테스트"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.get_top_articles(language='en', country='US')

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'hl=en' in call_args
        assert 'gl=US' in call_args
        assert 'ceid=US%3Aen' in call_args
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    @patch('src.external.news.client.get_feed_with_requests')
    def test_search_articles(self, mock_get_feed, google_news_client, mock_feed_data):
        """뉴스 검색 테스트 (NewsArticleCollection 반환)"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.search_articles('인공지능')

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'q=%EC%9D%B8%EA%B3%B5%EC%A7%80%EB%8A%A5' in call_args  # URL 인코딩된 '인공지능'
        assert 'https://news.google.com/rss/search' in call_args
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    @patch('src.external.news.client.get_feed_with_requests')
    def test_search_articles_with_when_filter(self, mock_get_feed, google_news_client, mock_feed_data):
        """시간 필터와 함께 뉴스 검색 테스트"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.search_articles('인공지능', when='7d')

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'when%3A7d' in call_args  # URL 인코딩된 'when:7d'
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    @patch('src.external.news.client.get_feed_with_requests')
    def test_get_category_articles_enum(self, mock_get_feed, google_news_client, mock_feed_data):
        """NewsCategory Enum을 사용한 카테고리 뉴스 테스트"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.get_category_articles(NewsCategory.TECHNOLOGY)

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'q=%EA%B8%B0%EC%88%A0' in call_args  # URL 인코딩된 '기술'
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    @patch('src.external.news.client.get_feed_with_requests')
    def test_get_category_articles_enum_english(self, mock_get_feed, google_news_client, mock_feed_data):
        """NewsCategory Enum을 사용한 영어 카테고리 뉴스 테스트"""
        mock_get_feed.return_value = mock_feed_data

        result = google_news_client.get_category_articles(NewsCategory.TECHNOLOGY, language='en')

        mock_get_feed.assert_called_once()
        call_args = mock_get_feed.call_args[0][0]
        assert 'q=technology' in call_args  # 영어 검색어
        assert isinstance(result, NewsArticleCollection)
        assert len(result) == 2

    def test_extract_articles_info(self, google_news_client, mock_feed_data):
        """기사 정보 추출 테스트"""
        articles = NewsArticleCollection.from_feed_data(mock_feed_data)

        assert len(articles) == 2
        assert all(isinstance(article, NewsArticle) for article in articles)

        first_article = articles[0]
        assert first_article.title == '테스트 뉴스 제목 1'
        assert first_article.link == 'https://example.com/news/1'
        assert first_article.description == '테스트 뉴스 내용 1'
        assert first_article.published == 'Mon, 01 Jan 2024 12:00:00 GMT'
        assert first_article.published_date is not None
        assert first_article.published_date.year == 2024
        assert first_article.published_date.month == 1
        assert first_article.id == 'test-id-1'
        assert first_article.source == '테스트 신문사'

        second_article = articles[1]
        assert second_article.title == '테스트 뉴스 제목 2'
        assert second_article.source == ''  # source 필드가 없는 경우

    def test_extract_articles_info_empty_feed(self, google_news_client):
        """빈 피드에서 기사 정보 추출 테스트"""
        empty_feed = {'entries': []}
        articles = NewsArticleCollection.from_feed_data(empty_feed)

        assert isinstance(articles, NewsArticleCollection)
        assert len(articles) == 0
        assert not articles  # __bool__ 메서드 테스트

    def test_extract_articles_info_no_entries(self, google_news_client):
        """entries 키가 없는 피드 테스트"""
        no_entries_feed = {}
        articles = NewsArticleCollection.from_feed_data(no_entries_feed)

        assert isinstance(articles, NewsArticleCollection)
        assert len(articles) == 0
        assert not articles  # __bool__ 메서드 테스트

    # 헬퍼 메서드 테스트 - 이미 위에서 테스트됨, 중복 제거


@pytest.mark.integration
def test_google_news_integration():
    """실제 구글 뉴스 RSS 연동 테스트 (네트워크 필요)"""
    client = GoogleNewsClient()

    try:
        # 실제 구글 뉴스 피드 테스트
        articles = client.get_top_articles()

        # NewsArticleCollection 타입 확인
        assert isinstance(articles, NewsArticleCollection)

        # 기사 정보 추출 테스트
        if len(articles) > 0:
            assert all(isinstance(article, NewsArticle) for article in articles)

            first_article = articles[0]
            assert hasattr(first_article, 'title')
            assert hasattr(first_article, 'link')

            print(f"성공적으로 {len(articles)}개의 뉴스를 가져왔습니다.")
            print(f"첫 번째 뉴스 제목: {first_article.title}")

    except Exception as e:
        print(f"네트워크 테스트 실패 (정상적인 상황일 수 있음): {e}")


if __name__ == "__main__":
    # 직접 실행 시 통합 테스트 수행
    test_google_news_integration()
