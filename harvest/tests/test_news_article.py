import sys
from datetime import datetime
from pathlib import Path

import pytest
from pydantic import ValidationError

sys.path.insert(0, str(Path(__file__).parent.parent))

from src.schemas.news import NewsArticle


class TestNewsArticle:
    """NewsArticle Pydantic 모델 테스트"""

    def test_basic_creation(self):
        """기본적인 뉴스 기사 생성 테스트"""
        article = NewsArticle(
            title="테스트 뉴스",
            link="https://example.com/news/1"
        )

        assert article.title == "테스트 뉴스"
        assert article.link == "https://example.com/news/1"
        assert article.description == ""
        assert article.published == ""
        assert article.published_date is None
        assert article.id == ""
        assert article.source == ""

    def test_full_creation(self):
        """모든 필드를 포함한 뉴스 기사 생성 테스트"""
        article = NewsArticle(
            title="완전한 테스트 뉴스",
            link="https://example.com/news/1",
            description="뉴스 설명입니다",
            published="Mon, 01 Jan 2024 12:00:00 GMT",
            published_date=(2024, 1, 1, 12, 0, 0),
            id="test-id-123",
            source="테스트 신문사"
        )

        assert article.title == "완전한 테스트 뉴스"
        assert article.link == "https://example.com/news/1"
        assert article.description == "뉴스 설명입니다"
        assert article.published == "Mon, 01 Jan 2024 12:00:00 GMT"
        assert article.published_date == datetime(2024, 1, 1, 12, 0, 0)
        assert article.id == "test-id-123"
        assert article.source == "테스트 신문사"

    def test_published_date_validation(self):
        """발행일 검증 테스트"""
        # 튜플 형태의 시간 데이터
        article1 = NewsArticle(
            title="테스트",
            link="https://example.com/1",
            published_date=(2024, 1, 1, 12, 0, 0, 0, 1, 0)
        )
        assert article1.published_date == datetime(2024, 1, 1, 12, 0, 0)

        # datetime 객체 직접 전달
        test_date = datetime(2023, 12, 31, 23, 59, 59)
        article2 = NewsArticle(
            title="테스트2",
            link="https://example.com/2",
            published_date=test_date
        )
        assert article2.published_date == test_date

        # None 값
        article3 = NewsArticle(
            title="테스트3",
            link="https://example.com/3",
            published_date=None
        )
        assert article3.published_date is None

    def test_validation_errors(self):
        """검증 오류 테스트"""
        # 빈 제목
        with pytest.raises(ValidationError) as exc_info:
            NewsArticle(title="", link="https://example.com/1")
        assert "제목은 비어있을 수 없습니다" in str(exc_info.value)

        # 빈 링크
        with pytest.raises(ValidationError) as exc_info:
            NewsArticle(title="테스트", link="")
        assert "링크는 비어있을 수 없습니다" in str(exc_info.value)

        # 공백만 있는 제목
        with pytest.raises(ValidationError) as exc_info:
            NewsArticle(title="   ", link="https://example.com/1")
        assert "제목은 비어있을 수 없습니다" in str(exc_info.value)

    def test_string_trimming(self):
        """문자열 공백 제거 테스트"""
        article = NewsArticle(
            title="  테스트 뉴스  ",
            link="  https://example.com/news/1  "
        )

        assert article.title == "테스트 뉴스"
        assert article.link == "https://example.com/news/1"

    def test_to_dict_method(self):
        """딕셔너리 변환 메서드 테스트"""
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        article = NewsArticle(
            title="테스트",
            link="https://example.com/1",
            description="설명",
            published="Mon, 01 Jan 2024 12:00:00 GMT",
            published_date=test_date,
            id="test-123",
            source="테스트 신문"
        )

        article_dict = article.to_dict()

        expected_dict = {
            'title': "테스트",
            'link': "https://example.com/1",
            'description': "설명",
            'published': "Mon, 01 Jan 2024 12:00:00 GMT",
            'published_parsed': test_date,
            'id': "test-123",
            'source': "테스트 신문"
        }

        assert article_dict == expected_dict

    def test_json_serialization(self):
        """JSON 직렬화 테스트"""
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        article = NewsArticle(
            title="테스트",
            link="https://example.com/1",
            published_date=test_date
        )

        json_data = article.model_dump_json()
        assert '"title":"테스트"' in json_data
        assert '"link":"https://example.com/1"' in json_data
        assert "2024-01-01T12:00:00" in json_data

    def test_invalid_published_date(self):
        """잘못된 발행일 데이터 테스트"""
        # 유효하지 않은 튜플 - 에러가 발생하지 않고 None으로 설정되어야 함
        article = NewsArticle(
            title="테스트",
            link="https://example.com/1",
            published_date=(2024, 13, 32)  # 잘못된 월, 일
        )
        assert article.published_date is None

        # 짧은 튜플 - None으로 설정되어야 함
        article2 = NewsArticle(
            title="테스트2",
            link="https://example.com/2",
            published_date=(2024,)  # 요소가 부족
        )
        assert article2.published_date is None


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
