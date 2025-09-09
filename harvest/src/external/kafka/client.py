"""중앙 설정을 사용하는 카프카 프로듀서 서비스"""

import json
import logging
from typing import Optional, Dict, Any, Callable

from confluent_kafka import Producer, KafkaError, KafkaException, Message

from .config import kafka_config
from ...schemas.kafka import DataCollectedMessage, KafkaMessageHeaders

logger = logging.getLogger(__name__)


class KafkaClient:
    """중앙 설정을 사용하는 카프카 프로듀서 서비스 클래스
    
    Context manager를 지원하여 리소스 관리를 안전하게 처리합니다.
    """

    def __init__(self):
        """카프카 프로듀서 초기화"""
        # 중앙 설정에서 프로듀서 설정 로드
        self.producer_config = kafka_config.producer_config()

        self._producer: Optional[Producer] = None
        self._connection_tested = False

        # 설정 검증
        self._validate_config()

        logger.info(f"카프카 설정 로드 완료: {kafka_config.bootstrap_servers}")

    def _validate_config(self) -> None:
        """카프카 설정 유효성 검사"""
        if not kafka_config.bootstrap_servers:
            raise ValueError("Bootstrap servers가 설정되지 않았습니다")

        required_config = ['bootstrap.servers']
        for key in required_config:
            if key not in self.producer_config:
                raise ValueError(f"필수 설정 '{key}'가 누락되었습니다")

    def __enter__(self):
        """Context manager 진입"""
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager 종료 - 리소스 정리"""
        self.close()

    def _get_producer(self) -> Producer:
        """프로듀서 인스턴스 반환 (지연 초기화)"""
        if self._producer is None:
            try:
                self._producer = Producer(self.producer_config)
                logger.info(f"카프카 프로듀서 초기화 완료: {kafka_config.bootstrap_servers}")
            except Exception as e:
                logger.error(f"카프카 프로듀서 초기화 실패: {e}")
                raise
        return self._producer

    def _delivery_callback(self, err: Optional[KafkaError], msg: Message) -> None:
        """메시지 전송 결과 콜백"""
        if err is not None:
            logger.error(f"메시지 전송 실패: {err}")
        else:
            logger.debug(f"메시지 전송 성공: {msg.topic()}[{msg.partition()}] offset={msg.offset()}")

    def test_connection(self) -> bool:
        """카프카 브로커 연결 테스트"""
        try:
            producer = self._get_producer()

            # 메타데이터 요청을 통한 연결 테스트 (timeout 5초)
            metadata = producer.list_topics(timeout=5.0)

            if metadata.topics:
                logger.info(f"카프카 연결 성공. 사용 가능한 토픽 수: {len(metadata.topics)}")
                self._connection_tested = True
                return True
            else:
                logger.warning("카프카 연결은 되었으나 토픽이 없습니다.")
                return False

        except KafkaException as e:
            logger.error(f"카프카 연결 테스트 실패: {e}")
            return False
        except KafkaException as e:
            logger.error(f"카프카 연결 테스트 실패: {e}")
            return False
        except TimeoutError as e:
            logger.error(f"카프카 연결 시간 초과: {e}")
            return False
        except Exception as e:
            logger.error(f"예상치 못한 오류로 카프카 연결 실패: {e}")
            return False

    def send_message(
            self,
            topic_logical_name: str,
            message: Dict[str, Any],
            key: Optional[str] = None,
            headers: Optional[Dict[str, str]] = None,
            callback: Optional[Callable] = None
    ) -> bool:
        """
        카프카 토픽에 메시지 발송 (논리적 토픽명 사용)
        
        Args:
            topic_logical_name: 논리적 토픽명 (예: 'data-collected')
            message: 메시지 내용 (딕셔너리)
            key: 메시지 키 (파티셔닝용)
            headers: 메시지 헤더
            callback: 전송 완료 콜백 함수
        
        Returns:
            메시지 큐에 추가 성공 시 True
        """
        try:
            # 논리적 토픽명을 실제 토픽명으로 변환
            actual_topic = kafka_config.topic_name(topic_logical_name)

            producer = self._get_producer()

            # 메시지를 JSON으로 직렬화
            message_json = json.dumps(message, ensure_ascii=False, default=str)

            # 헤더 준비 (있는 경우)
            kafka_headers = None
            if headers:
                kafka_headers = [(k, v.encode('utf-8')) for k, v in headers.items()]

            # 콜백 설정
            delivery_callback = callback or self._delivery_callback

            # 메시지 발송
            producer.produce(
                topic=actual_topic,
                key=key,
                value=message_json.encode('utf-8'),
                headers=kafka_headers,
                callback=delivery_callback
            )

            # 큐에 있는 메시지 플러시 (논블로킹, 최대 1초 대기)
            producer.poll(1.0)

            logger.debug(f"메시지 전송 대기열에 추가됨: {topic_logical_name} -> {actual_topic}")
            return True

        except ValueError as e:
            logger.error(f"잘못된 토픽명 또는 메시지 형식: {e}", extra={"topic": topic_logical_name})
            return False
        except KafkaException as e:
            logger.error(f"카프카 메시지 발송 실패: {e}", extra={"topic": topic_logical_name, "error_code": e.args[0].code() if e.args else None})
            return False
        except json.JSONEncodeError as e:
            logger.error(f"메시지 JSON 직렬화 실패: {e}", extra={"topic": topic_logical_name})
            return False
        except Exception as e:
            logger.error(f"메시지 발송 중 예상치 못한 오류: {e}", extra={"topic": topic_logical_name})
            return False

    def send_data_collected(
            self,
            message: DataCollectedMessage,
            callback: Optional[Callable] = None
    ) -> bool:
        """
        데이터 수집 완료 메시지 발송 (중앙 설정 사용)
        
        Args:
            message: 수집 완료 메시지
            callback: 전송 완료 콜백 함수
            
        Returns:
            발송 성공 시 True
        """
        # 스키마 버전을 중앙 설정에서 가져옴
        schema_version = kafka_config.schema_version("data-collected")

        # 메시지 헤더 생성
        headers = KafkaMessageHeaders(
            message_type="data-collected",
            version=schema_version,
            source_service="harvest"
        )

        # 메시지 키는 data_id로 설정 (같은 데이터는 같은 파티션으로)
        message_key = str(message.data_id)

        # 헤더를 딕셔너리로 변환
        headers_dict = headers.model_dump()
        headers_dict = {k: str(v) for k, v in headers_dict.items()}

        # 논리적 토픽명으로 전송
        return self.send_message(
            topic_logical_name="data-collected",
            message=message.model_dump(),
            key=message_key,
            headers=headers_dict,
            callback=callback
        )

    def flush(self, timeout: float = 10.0) -> int:
        """
        대기 중인 모든 메시지 플러시
        
        Args:
            timeout: 최대 대기 시간 (초)
            
        Returns:
            플러시되지 못한 메시지 수 (0이면 모든 메시지 전송 완료)
        """
        try:
            if self._producer:
                remaining = self._producer.flush(timeout)
                if remaining == 0:
                    logger.info("모든 메시지가 성공적으로 전송되었습니다.")
                else:
                    logger.warning(f"{remaining}개의 메시지가 전송되지 못했습니다.")
                return remaining
            return 0
        except Exception as e:
            logger.error(f"메시지 플러시 중 오류 발생: {e}")
            return -1

    def close(self):
        """프로듀서 연결 종료"""
        try:
            if self._producer:
                # 남은 메시지 모두 전송
                self.flush(timeout=5.0)
                self._producer = None
                logger.info("카프카 프로듀서 연결이 종료되었습니다.")
        except Exception as e:
            logger.error(f"카프카 프로듀서 종료 중 오류 발생: {e}")

    def __del__(self):
        """소멸자에서 연결 정리"""
        self.close()

    @property
    def available_topics(self) -> Dict[str, Any]:
        """사용 가능한 토픽 목록과 설정 반환"""
        return {
            logical_name: {
                "actual_name": settings.name,
                "partitions": settings.partitions,
                "description": settings.description
            }
            for logical_name, settings in kafka_config.topics.topics.items()
        }
