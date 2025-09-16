import logging
from typing import List

from src.external.kafka.producer import DocumentProducer, KafkaProducerConfig
from src.schemas.document import CreateDocumentRequest


logger = logging.getLogger(__name__)


class MessagePublisher:
    """문서 메시지 발행 서비스"""
    
    def __init__(self, kafka_config: KafkaProducerConfig):
        self.producer = DocumentProducer(kafka_config)
    
    def publish_document_requests(
        self, 
        documents: List[CreateDocumentRequest]
    ) -> bool:
        """
        문서 생성 요청들을 카프카로 발행 (문서 타입별로 각각 다른 토픽 사용)
        
        Args:
            documents: 발행할 문서 요청 리스트
            
        Returns:
            bool: 발행 성공 여부
        """
        if not documents:
            logger.warning("No documents to publish")
            return False
            
        logger.info(f"Publishing {len(documents)} document requests")
        
        # 문서 타입별 토픽 매핑
        topic_mapping = {
            "WIKI": "document-index-wiki",
            "NEWS": "document-index-news", 
            "TWEET": "document-index-tweet"
        }
        
        # 문서 타입별로 분리해서 발행
        document_groups = {}
        for doc in documents:
            doc_type = doc.document_type
            if doc_type not in document_groups:
                document_groups[doc_type] = []
            document_groups[doc_type].append(doc)
        
        success_count = 0
        for doc_type, docs in document_groups.items():
            topic = topic_mapping[doc_type.value]
            logger.info(f"Publishing {len(docs)} {doc_type.value} documents to topic '{topic}'")
            
            success = self.producer.publish_documents(
                documents=docs,
                topic=topic,
                partition_key=doc_type.value
            )
            
            if success:
                success_count += len(docs)
            else:
                logger.error(f"Failed to publish {doc_type.value} documents to topic '{topic}'")
        
        total_count = len(documents)
        logger.info(f"Published {success_count}/{total_count} documents successfully")
        
        return success_count == total_count
    
    def close(self):
        """리소스 정리"""
        self.producer.close()