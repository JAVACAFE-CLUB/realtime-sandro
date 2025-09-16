package com.sandro.realtime.codex.consumer

import com.sandro.realtime.codex.dto.CreateDocumentRequest
import com.sandro.realtime.codex.service.DocumentService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class DocumentIndexConsumer(
    private val documentService: DocumentService
) {
    private val logger = LoggerFactory.getLogger(DocumentIndexConsumer::class.java)

    @KafkaListener(
        topics = ["document-index-news"],
        containerFactory = "batchKafkaListenerContainerFactory",
        groupId = "codex-news-consumer",
    )
    fun consumeNewsDocuments(messages: List<ConsumerRecord<String, CreateDocumentRequest>>) {
        try {
            logger.info("뉴스 문서 배치 색인 요청 수신: ${messages.size}개")
            val documents = documentService.createDocumentsBatch(messages.map { consumerRecord -> consumerRecord.value() })
            logger.info("뉴스 문서 배치 색인 완료: ${documents.size}개 처리")

            if (logger.isDebugEnabled) {
                documents.forEach { doc ->
                    logger.debug("뉴스 문서 색인됨: ID=${doc.id}, 제목=${doc.title}")
                }
            }
        } catch (e: Exception) {
            logger.error("뉴스 문서 배치 색인 실패: ${messages.size}개 중 실패", e)
            throw e
        }
    }

    @KafkaListener(
        topics = ["document-index-wiki"],
        containerFactory = "batchKafkaListenerContainerFactory",
        groupId = "codex-wiki-consumer",
    )
    fun consumeWikiDocuments(messages: List<CreateDocumentRequest>) {
        try {
            logger.info("위키 문서 배치 색인 요청 수신: ${messages.size}개")
            val documents = documentService.createDocumentsBatch(messages)
            logger.info("위키 문서 배치 색인 완료: ${documents.size}개 처리")

            if (logger.isDebugEnabled) {
                documents.forEach { doc ->
                    logger.debug("위키 문서 색인됨: ID=${doc.id}, 제목=${doc.title}")
                }
            }
        } catch (e: Exception) {
            logger.error("위키 문서 배치 색인 실패: ${messages.size}개 중 실패", e)
            throw e
        }
    }

    @KafkaListener(
        topics = ["document-index-tweet"],
        containerFactory = "batchKafkaListenerContainerFactory",
        groupId = "codex-tweet-consumer",
    )
    fun consumeTweetDocuments(messages: List<CreateDocumentRequest>) {
        try {
            logger.info("트윗 문서 배치 색인 요청 수신: ${messages.size}개")
            val documents = documentService.createDocumentsBatch(messages)
            logger.info("트윗 문서 배치 색인 완료: ${documents.size}개 처리")

            if (logger.isDebugEnabled) {
                documents.forEach { doc ->
                    logger.debug("트윗 문서 색인됨: ID=${doc.id}, 작성자=${doc.author}")
                }
            }
        } catch (e: Exception) {
            logger.error("트윗 문서 배치 색인 실패: ${messages.size}개 중 실패", e)
            throw e
        }
    }
}