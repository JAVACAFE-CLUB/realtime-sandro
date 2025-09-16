import json
import logging
from typing import List, Optional

from confluent_kafka import Producer

from src.schemas.document import CreateDocumentRequest

logger = logging.getLogger(__name__)


class KafkaProducerConfig:
    """카프카 프로듀서 설정"""
    def __init__(
        self,
        bootstrap_servers: str = "localhost:9092",
        client_id: str = "smithy"
    ):
        self.bootstrap_servers = bootstrap_servers
        self.client_id = client_id


def _delivery_callback(error, message):
    """메시지 전송 결과 콜백"""
    if error:
        logger.error(f"Message delivery failed: {error}")
    else:
        logger.info(f"Message delivered to topic '{message.topic()}' partition {message.partition()}")


class DocumentProducer:
    """문서 요청을 카프카로 발행하는 프로듀서"""
    
    def __init__(self, config: KafkaProducerConfig):
        self.config = config
        self.producer = Producer({
            'bootstrap.servers': config.bootstrap_servers,
            'client.id': config.client_id,
        })
        
    def publish_documents(
        self, 
        documents: List[CreateDocumentRequest], 
        topic: str = "document-requests",
        partition_key: Optional[str] = None
    ) -> bool:
        """
        문서 생성 요청 리스트를 카프카로 발행
        
        Args:
            documents: 발행할 문서 요청 리스트
            topic: 카프카 토픽명
            partition_key: 파티션 키 (없으면 document_type 사용)
            
        Returns:
            bool: 발행 성공 여부
        """
        try:
            # Pydantic 모델을 dict로 변환 후 JSON 직렬화
            message_data = [doc.model_dump() for doc in documents]
            message_json = json.dumps(message_data, ensure_ascii=False, default=str)
            
            # 파티션 키 설정 - 제공되지 않으면 첫 번째 문서의 타입 사용
            if partition_key is None and documents:
                partition_key = documents[0].document_type.value
            
            # 메시지 발행
            self.producer.produce(
                topic=topic,
                key=partition_key,
                value=message_json.encode('utf-8'),
                callback=_delivery_callback
            )
            
            # 메시지 전송 대기
            self.producer.flush()
            
            logger.info(f"Successfully published {len(documents)} documents to topic '{topic}'")
            return True
            
        except Exception as e:
            logger.error(f"Failed to publish documents: {e}")
            return False

    def close(self):
        """프로듀서 연결 종료"""
        self.producer.flush()
        logger.info("Kafka producer closed")