"""X API 라우터"""
from typing import List

from fastapi import APIRouter, HTTPException, Query, Depends, status

from ...external.x import XAPIError
from ...schemas.x import (
    TweetSearchRequest,
    UserTimelineRequest,
    TweetResponse,
    TweetListResponse,
)
from ...services.x_service import XService

router = APIRouter()


def get_x_service() -> XService:
    """X 서비스 의존성 주입"""
    try:
        return XService()
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"X 서비스 초기화 실패: {str(e)}"
        )


@router.post("/search", response_model=List[TweetResponse])
async def search_tweets(
        request: TweetSearchRequest,
        x_service: XService = Depends(get_x_service)
) -> List[TweetResponse]:
    """트윗 검색
    
    검색 쿼리 예시:
    - "python": python 키워드 포함
    - "python lang:ko": 한국어 python 트윗
    - "from:elonmusk": 특정 사용자의 트윗
    - "#AI -is:retweet": AI 해시태그, 리트윗 제외
    """
    if not x_service.is_connected():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="X API가 설정되지 않았습니다. 환경변수를 확인하세요."
        )

    try:
        results = x_service.search_tweets(
            query=request.query,
            max_results=request.max_results,
            start_time=request.start_time,
            end_time=request.end_time
        )

        return [TweetResponse(**tweet) for tweet in results]

    except XAPIError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"트윗 검색 실패: {str(e)}"
        )


@router.post("/timeline", response_model=List[TweetResponse])
async def get_user_timeline(
        request: UserTimelineRequest,
        x_service: XService = Depends(get_x_service)
) -> List[TweetResponse]:
    """사용자 타임라인 가져오기"""
    if not x_service.is_connected():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="X API가 설정되지 않았습니다. 환경변수를 확인하세요."
        )

    try:
        results = x_service.get_user_timeline(
            username=request.username,
            max_results=request.max_results,
            exclude_replies=request.exclude_replies,
            exclude_retweets=request.exclude_retweets
        )

        return [TweetResponse(**tweet) for tweet in results]

    except XAPIError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"타임라인 가져오기 실패: {str(e)}"
        )


@router.get("/search/simple", response_model=TweetListResponse)
async def search_tweets_simple(
        q: str = Query(..., description="검색 쿼리"),
        count: int = Query(default=10, ge=10, le=100, description="결과 수"),
        x_service: XService = Depends(get_x_service)
) -> TweetListResponse:
    """간단한 트윗 검색 (GET 방식)"""
    if not x_service.is_connected():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="X API가 설정되지 않았습니다. 환경변수를 확인하세요."
        )

    try:
        results = x_service.search_tweets(
            query=q,
            max_results=count
        )

        tweets = [TweetResponse(**tweet) for tweet in results]

        return TweetListResponse(
            tweets=tweets,
            count=len(tweets),
            query=q
        )

    except XAPIError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"트윗 검색 실패: {str(e)}"
        )
