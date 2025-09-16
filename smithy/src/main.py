import logging
from datetime import datetime
from typing import List

from src.external.kafka.producer import KafkaProducerConfig
from src.schemas.document import CreateDocumentRequest, DocumentType
from src.services.message_publisher import MessagePublisher


# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def create_sample_documents() -> List[CreateDocumentRequest]:
    """샘플 문서 데이터 생성"""
    
    # 뉴스 기사 샘플 데이터
    news_document = CreateDocumentRequest(
        document_type=DocumentType.NEWS,
        title="핵잠수함도 샅샅이 찾아낸다…中연구팀 \"AI기술로 95% 추적 성공\"",
        content="""중국의 해상 전력에 가장 큰 위협으로 꼽혀온 저소음 잠수함이 인공지능(AI) 기술의 비약적 발전으로 거의 무력화될 수도 있다는 중국 연구팀의 주장이 나왔다. 15일 홍콩 일간 사우스차이나모닝포스트(SCMP)에 따르면""",
        source="디지털타임스",
        author="디지털타임스",
        published_at=datetime(2025, 9, 15, 14, 7, 12),
        url="https://n.news.naver.com/article/029/0002982202",
        categories=["국제", "과학기술"],
        tags=["AI", "잠수함", "중국", "군사기술"],
        metadata={
            "og_title": "핵잠수함도 샅샅이 찾아낸다…中연구팀 \"AI기술로 95% 추적 성공\"",
            "og_image": "https://imgnews.pstatic.net/image/029/2025/09/15/0002982202_001_20250915140712623.png",
            "source_type": "naver_news"
        }
    )
    
    # 위키피디아 페이지 샘플 데이터
    wiki_document = CreateDocumentRequest(
        document_type=DocumentType.WIKI,
        title="지미 카터",
        content="""제임스 얼 "지미" 카터 주니어(James Earl "Jimmy" Carter Jr., 1924년 10월 1일~)는 미국의 정치인이자 제39대 미국 대통령(1977~1981)이다. 그는 조지아주 출신으로 민주당 소속이며, 대통령 재임 이전에는 조지아주 주지사(1971~1975)를 역임했다.""",
        author="TedBot",
        published_at=datetime(2025, 7, 19, 13, 30, 53),
        url="https://ko.wikipedia.org/wiki/지미_카터",
        categories=["미국 대통령", "정치인"],
        tags=["미국", "대통령", "민주당", "조지아주"],
        metadata={
            "page_id": "5",
            "revision_id": "40301269",
            "parent_id": "40096343",
            "namespace": "0",
            "contributor_id": "368112",
            "source_type": "wikipedia"
        }
    )
    
    return [news_document, wiki_document]


def main():
    """메인 실행 함수"""
    logger.info("Smithy 문서 메시지 발행 시작")
    
    try:
        # 카프카 설정
        kafka_config = KafkaProducerConfig(
            bootstrap_servers="localhost:9092",
            client_id="smithy-producer"
        )
        
        # 메시지 발행 서비스 생성
        publisher = MessagePublisher(kafka_config)
        
        # 샘플 문서 데이터 생성
        documents = create_sample_documents()
        logger.info(f"생성된 샘플 문서 수: {len(documents)}")
        
        for i, doc in enumerate(documents, 1):
            logger.info(f"문서 {i}: {doc.document_type.value} - {doc.title}")
        
        # 카프카로 메시지 발행 (문서 타입별로 각각 다른 토픽에 발행)
        success = publisher.publish_document_requests(
            documents=documents
        )
        
        if success:
            logger.info("모든 문서 메시지가 성공적으로 발행되었습니다")
        else:
            logger.error("일부 문서 메시지 발행에 실패했습니다")
        
        # 리소스 정리
        publisher.close()
        
    except Exception as e:
        logger.error(f"실행 중 오류 발생: {e}")
        raise


if __name__ == "__main__":
    main()