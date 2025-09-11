"""X API 서비스"""
import logging
from datetime import datetime, timedelta, UTC
from typing import List, Dict, Any, Optional

from ..core.config import settings
from ..external.x import get_x_api_client, XAPIError

logger = logging.getLogger(__name__)


class XService:
    """X API 서비스 클래스"""

    def __init__(self):
        """서비스 초기화"""
        self.client = get_x_api_client()

    def search_tweets(
            self,
            query: str,
            max_results: int = 10,
            start_time: Optional[datetime] = None,
            end_time: Optional[datetime] = None
    ) -> List[Dict[str, Any]]:
        """트윗 검색
        
        Args:
            query: 검색 쿼리 (예: "python lang:ko")
            max_results: 최대 결과 수
            start_time: 검색 시작 시간
            end_time: 검색 종료 시간
            
        Returns:
            트윗 목록
        """
        try:
            # 기본 시간 설정 (최근 7일)
            if not start_time:
                start_time = datetime.now(UTC) - timedelta(days=7)

            # 트윗 필드 설정
            tweet_fields = [
                'id', 'text', 'created_at', 'author_id',
                'lang', 'public_metrics', 'entities'
            ]

            # 검색 실행
            tweets = self.client.search_recent_tweets(
                query=query,
                max_results=min(max_results, settings.x_max_results),
                start_time=start_time,
                end_time=end_time,
                tweet_fields=tweet_fields
            )

            if not tweets or not tweets.data:
                logger.info(f"검색 결과가 없습니다: {query}")
                return []

            # 결과 파싱
            results = []
            for tweet in tweets.data:
                results.append({
                    'id': tweet.id,
                    'text': tweet.text,
                    'created_at': tweet.created_at.isoformat() if tweet.created_at else None,
                    'author_id': tweet.author_id,
                    'lang': tweet.lang if hasattr(tweet, 'lang') else None,
                    'metrics': tweet.public_metrics if hasattr(tweet, 'public_metrics') else None
                })

            logger.info(f"{len(results)}개의 트윗을 검색했습니다.")
            return results

        except XAPIError:
            raise
        except Exception as e:
            logger.error(f"트윗 검색 실패: {e}")
            raise XAPIError(f"트윗 검색 실패: {str(e)}")

    def get_user_timeline(
            self,
            username: str,
            max_results: int = 10,
            exclude_replies: bool = True,
            exclude_retweets: bool = True
    ) -> List[Dict[str, Any]]:
        """사용자 타임라인 가져오기
        
        Args:
            username: 사용자명 (@ 제외)
            max_results: 최대 결과 수
            exclude_replies: 답글 제외 여부
            exclude_retweets: 리트윗 제외 여부
            
        Returns:
            트윗 목록
        """
        try:
            # 사용자 ID 조회
            user = self.client.get_user(username=username)
            if not user or not user.data:
                logger.error(f"사용자를 찾을 수 없습니다: {username}")
                return []

            user_id = user.data.id

            # 트윗 필드 설정
            tweet_fields = [
                'id', 'text', 'created_at', 'author_id',
                'lang', 'public_metrics', 'entities'
            ]

            # 제외 옵션 설정
            exclude = []
            if exclude_replies:
                exclude.append('replies')
            if exclude_retweets:
                exclude.append('retweets')

            # 타임라인 가져오기
            tweets = self.client.get_users_tweets(
                user_id=user_id,
                max_results=min(max_results, settings.x_max_results),
                tweet_fields=tweet_fields,
                exclude=exclude if exclude else None
            )

            if not tweets or not tweets.data:
                logger.info(f"타임라인이 비어있습니다: {username}")
                return []

            # 결과 파싱
            results = []
            for tweet in tweets.data:
                results.append({
                    'id': tweet.id,
                    'text': tweet.text,
                    'created_at': tweet.created_at.isoformat() if tweet.created_at else None,
                    'author_id': tweet.author_id,
                    'username': username,
                    'lang': tweet.lang if hasattr(tweet, 'lang') else None,
                    'metrics': tweet.public_metrics if hasattr(tweet, 'public_metrics') else None
                })

            logger.info(f"{username}의 트윗 {len(results)}개를 가져왔습니다.")
            return results

        except XAPIError:
            raise
        except Exception as e:
            logger.error(f"타임라인 가져오기 실패: {e}")
            raise XAPIError(f"타임라인 가져오기 실패: {str(e)}")

    def is_connected(self) -> bool:
        """API 연결 상태 확인"""
        return self.client.is_connected()

    def is_configured(self) -> bool:
        """API 설정 상태 확인"""
        return self.client.is_configured()
