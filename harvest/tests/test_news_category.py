import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from src.external.news.categories import NewsCategory


class TestNewsCategory:
    """NewsCategory Enum 테스트"""

    def test_enum_values(self):
        """Enum 값들이 올바르게 정의되어 있는지 테스트"""
        assert NewsCategory.WORLD.value == "WORLD"
        assert NewsCategory.NATION.value == "NATION"
        assert NewsCategory.BUSINESS.value == "BUSINESS"
        assert NewsCategory.TECHNOLOGY.value == "TECHNOLOGY"
        assert NewsCategory.ENTERTAINMENT.value == "ENTERTAINMENT"
        assert NewsCategory.SPORTS.value == "SPORTS"
        assert NewsCategory.SCIENCE.value == "SCIENCE"
        assert NewsCategory.HEALTH.value == "HEALTH"

    def test_korean_queries(self):
        """한국어 검색어 테스트"""
        assert NewsCategory.WORLD.korean_query == '세계'
        assert NewsCategory.NATION.korean_query == '국내'
        assert NewsCategory.BUSINESS.korean_query == '경제'
        assert NewsCategory.TECHNOLOGY.korean_query == '기술'
        assert NewsCategory.ENTERTAINMENT.korean_query == '연예'
        assert NewsCategory.SPORTS.korean_query == '스포츠'
        assert NewsCategory.SCIENCE.korean_query == '과학'
        assert NewsCategory.HEALTH.korean_query == '건강'

    def test_english_queries(self):
        """영어 검색어 테스트"""
        assert NewsCategory.WORLD.english_query == 'world'
        assert NewsCategory.NATION.english_query == 'domestic'
        assert NewsCategory.BUSINESS.english_query == 'business'
        assert NewsCategory.TECHNOLOGY.english_query == 'technology'
        assert NewsCategory.ENTERTAINMENT.english_query == 'entertainment'
        assert NewsCategory.SPORTS.english_query == 'sports'
        assert NewsCategory.SCIENCE.english_query == 'science'
        assert NewsCategory.HEALTH.english_query == 'health'

    def test_get_query_for_language(self):
        """언어별 검색어 반환 테스트"""
        tech = NewsCategory.TECHNOLOGY

        # 한국어
        assert tech.get_query_for_language('ko') == '기술'
        assert tech.get_query_for_language() == '기술'  # 기본값

        # 영어
        assert tech.get_query_for_language('en') == 'technology'

        # 지원하지 않는 언어
        assert tech.get_query_for_language('fr') == 'technology'  # 소문자 영어 반환

    def test_from_string_valid(self):
        """유효한 문자열로부터 Enum 생성 테스트"""
        assert NewsCategory.from_string('WORLD') == NewsCategory.WORLD
        assert NewsCategory.from_string('world') == NewsCategory.WORLD  # 대소문자 무관
        assert NewsCategory.from_string('Technology') == NewsCategory.TECHNOLOGY

    def test_from_string_invalid(self):
        """유효하지 않은 문자열 처리 테스트"""
        with pytest.raises(ValueError) as exc_info:
            NewsCategory.from_string('INVALID')

        assert "지원하지 않는 카테고리입니다" in str(exc_info.value)
        assert "INVALID" in str(exc_info.value)
        # 지원 카테고리 목록이 포함되어 있는지 확인
        assert "WORLD" in str(exc_info.value)

    def test_get_all_categories(self):
        """모든 카테고리 매핑 반환 테스트"""
        all_categories = NewsCategory.get_all_categories()

        assert isinstance(all_categories, dict)
        assert all_categories['WORLD'] == '세계'
        assert all_categories['TECHNOLOGY'] == '기술'
        assert all_categories['SPORTS'] == '스포츠'

        # 모든 카테고리가 포함되어 있는지 확인
        assert len(all_categories) == 8

    def test_str_representation(self):
        """문자열 표현 테스트"""
        assert str(NewsCategory.TECHNOLOGY) == "TECHNOLOGY"
        assert str(NewsCategory.SPORTS) == "SPORTS"

    def test_iteration(self):
        """Enum 반복 테스트"""
        categories = list(NewsCategory)
        assert len(categories) == 8
        assert NewsCategory.WORLD in categories
        assert NewsCategory.HEALTH in categories

    def test_comparison(self):
        """Enum 비교 테스트"""
        tech1 = NewsCategory.TECHNOLOGY
        tech2 = NewsCategory.TECHNOLOGY
        sports = NewsCategory.SPORTS

        assert tech1 == tech2
        assert tech1 != sports
        assert tech1 is tech2  # 동일한 객체


class TestNewsCategoryIntegration:
    """NewsCategory와 다른 컴포넌트 통합 테스트"""

    def test_all_categories_have_translations(self):
        """모든 카테고리가 한국어와 영어 번역을 가지고 있는지 테스트"""
        korean_queries = NewsCategory._korean_queries()
        english_queries = NewsCategory._english_queries()

        for category in NewsCategory:
            assert category in korean_queries, f"{category}의 한국어 번역이 없습니다"
            assert category in english_queries, f"{category}의 영어 번역이 없습니다"
            assert korean_queries[category], f"{category}의 한국어 번역이 비어있습니다"
            assert english_queries[category], f"{category}의 영어 번역이 비어있습니다"

    def test_no_duplicate_translations(self):
        """번역에 중복이 없는지 테스트"""
        korean_values = list(NewsCategory._korean_queries().values())
        english_values = list(NewsCategory._english_queries().values())

        assert len(korean_values) == len(set(korean_values)), "한국어 번역에 중복이 있습니다"
        assert len(english_values) == len(set(english_values)), "영어 번역에 중복이 있습니다"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
