import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from src.external.news.client import GoogleNewsClient
from src.external.news.categories import NewsCategory


@pytest.mark.integration
def test_google_news_client():
    """구글 뉴스 클라이언트 통합 테스트 (실제 API 호출)"""
    client = GoogleNewsClient()

    # 톱 뉴스 테스트 (헬퍼 메서드 사용)
    print("=== 톱 뉴스 테스트 ===")
    articles = client.get_top_articles()

    print(f"톱 뉴스 {len(articles)}개 발견")
    if articles:
        print(f"첫 번째 뉴스: {articles[0].title}")
        print(f"링크: {articles[0].link}")
        print(f"발행일: {articles[0].published}")
        print(f"발행일 (datetime): {articles[0].published_date}")

    # 검색 테스트 (헬퍼 메서드 사용)
    print("\n=== 검색 테스트 ===")
    search_articles = client.search_articles('인공지능', when='7d')

    print(f"'인공지능' 검색 결과 {len(search_articles)}개 발견")
    if search_articles:
        print(f"첫 번째 검색 결과: {search_articles[0].title}")

    # 카테고리 테스트 (헬퍼 메서드 사용)
    print("\n=== 카테고리 테스트 (Enum) ===")
    tech_articles = client.get_category_articles(NewsCategory.TECHNOLOGY)

    print(f"기술 뉴스 {len(tech_articles)}개 발견")
    if tech_articles:
        print(f"첫 번째 기술 뉴스: {tech_articles[0].title}")

    # 스포츠 카테고리 테스트 (헬퍼 메서드 사용)
    print("\n=== 스포츠 카테고리 테스트 ===")
    sports_articles = client.get_category_articles(NewsCategory.SPORTS)

    print(f"스포츠 뉴스 {len(sports_articles)}개 발견")
    if sports_articles:
        print(f"첫 번째 스포츠 뉴스: {sports_articles[0].title}")

    # 영어 카테고리 테스트 (헬퍼 메서드 사용)
    print("\n=== 영어 카테고리 테스트 ===")
    business_articles = client.get_category_articles(NewsCategory.BUSINESS, language='en', country='US')

    print(f"비즈니스 뉴스 (영어) {len(business_articles)}개 발견")
    if business_articles:
        print(f"첫 번째 비즈니스 뉴스: {business_articles[0].title}")


if __name__ == "__main__":
    print("구글 뉴스 클라이언트 테스트를 시작합니다...")
    try:
        test_google_news_client()
        print("\n테스트 완료!")
    except Exception as e:
        print(f"테스트 중 오류 발생: {e}")
