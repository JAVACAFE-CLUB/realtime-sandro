"""X API 클라이언트"""
import logging
from datetime import datetime
from typing import Optional, List, Any

from tweepy import Client

from .exceptions import (
    XAPIError,
    XAPIConnectionError,
    XAPIConfigError,
)
from ...core.config import settings

logger = logging.getLogger(__name__)


class XAPIClient:
    """X API 클라이언트 클래스"""

    def __init__(self):
        """X API 클라이언트 초기화"""
        self.client: Optional[Client] = None
        self._initialize_client()

    def _initialize_client(self):
        """X API 클라이언트 초기화"""
        if not self.is_configured():
            logger.warning("X API 설정이 완료되지 않았습니다.")
            return

        try:
            # Bearer Token 인증 (읽기 전용)
            if settings.x_bearer_token:
                self.client = Client(
                    bearer_token=settings.x_bearer_token,
                    wait_on_rate_limit=settings.x_rate_limit_wait
                )

            # OAuth 1.0a User Context (읽기/쓰기)
            elif settings.x_access_token and settings.x_access_token_secret:
                self.client = Client(
                    consumer_key=settings.x_api_key,
                    consumer_secret=settings.x_api_secret,
                    access_token=settings.x_access_token,
                    access_token_secret=settings.x_access_token_secret,
                    wait_on_rate_limit=settings.x_rate_limit_wait
                )
            else:
                raise XAPIConfigError("X API 인증 정보가 부족합니다.")

        except Exception as e:
            logger.error(f"X API 클라이언트 초기화 실패: {e}")
            raise XAPIConnectionError(f"X API 연결 실패: {str(e)}")

    def search_recent_tweets(
            self,
            query: str,
            max_results: int = 10,
            start_time: Optional[datetime] = None,
            end_time: Optional[datetime] = None,
            tweet_fields: Optional[List[str]] = None
    ) -> Optional[Any]:
        """최근 트윗 검색"""
        if not self.client:
            raise XAPIConnectionError("X API 클라이언트가 초기화되지 않았습니다.")

        try:
            return self.client.search_recent_tweets(
                query=query,
                max_results=max_results,
                start_time=start_time,
                end_time=end_time,
                tweet_fields=tweet_fields
            )
        except Exception as e:
            logger.error(f"트윗 검색 실패: {e}")
            raise XAPIError(f"트윗 검색 실패: {str(e)}")

    def get_user(self, username: str) -> Optional[Any]:
        """사용자 정보 조회"""
        if not self.client:
            raise XAPIConnectionError("X API 클라이언트가 초기화되지 않았습니다.")

        try:
            return self.client.get_user(username=username)
        except Exception as e:
            logger.error(f"사용자 조회 실패: {e}")
            raise XAPIError(f"사용자 조회 실패: {str(e)}")

    def get_users_tweets(
            self,
            user_id: str,
            max_results: int = 10,
            tweet_fields: Optional[List[str]] = None,
            exclude: Optional[List[str]] = None
    ) -> Optional[Any]:
        """사용자 트윗 조회"""
        if not self.client:
            raise XAPIConnectionError("X API 클라이언트가 초기화되지 않았습니다.")

        try:
            return self.client.get_users_tweets(
                id=user_id,
                max_results=max_results,
                tweet_fields=tweet_fields,
                exclude=exclude
            )
        except Exception as e:
            logger.error(f"사용자 트윗 조회 실패: {e}")
            raise XAPIError(f"사용자 트윗 조회 실패: {str(e)}")

    def is_connected(self) -> bool:
        """X API 연결 상태 확인"""
        return self.client is not None

    @staticmethod
    def is_configured() -> bool:
        """X API가 설정되었는지 확인"""
        return bool(
            settings.x_bearer_token or
            (settings.x_api_key and settings.x_api_secret)
        )


# 싱글톤 인스턴스
_x_api_client = None


def get_x_api_client() -> XAPIClient:
    """X API 클라이언트 싱글톤 인스턴스 반환"""
    global _x_api_client
    if _x_api_client is None:
        _x_api_client = XAPIClient()
    return _x_api_client
