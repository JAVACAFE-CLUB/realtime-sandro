package com.sandro.realtime.harvest.common.service

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import com.sandro.realtime.harvest.news.event.NewsArticlesBatchProcessedEvent
import com.sandro.realtime.harvest.wiki.event.WikiPagesBatchProcessedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class ContentKafkaService(
    private val kafkaTemplate: KafkaTemplate<String, ContentProcessedMessage>
) {

    private val logger = LoggerFactory.getLogger(ContentKafkaService::class.java)

    /**
     * WikiPage 배치 처리 완료 이벤트 처리 (비동기)
     */
    @EventListener
    fun handleWikiPagesBatchProcessed(event: WikiPagesBatchProcessedEvent) {
        try {
            val kafkaMessages = event.pages.map(ContentProcessedMessage::from)

            sendBatchMessages(KafkaTopic.WIKI_CONTENT_PROCESSED, kafkaMessages)
        } catch (e: Exception) {
            logger.error("Failed to send Kafka messages for wiki batch", e)
            // 카프카 전송 실패는 메인 프로세스에 영향을 주지 않음
        }
    }

    /**
     * NewsArticle 배치 처리 완료 이벤트 처리 (비동기)
     */
    @EventListener
    fun handleNewsArticlesBatchProcessed(event: NewsArticlesBatchProcessedEvent) {
        try {
            val kafkaMessages = event.pages.map(ContentProcessedMessage::from)

            // 배치로 메시지 전송 (대량 데이터 처리 시 효율적)
            sendBatchMessages(KafkaTopic.NEWS_CONTENT_PROCESSED, kafkaMessages)
        } catch (e: Exception) {
            logger.error("Failed to send Kafka messages for news batch", e)
            // 카프카 전송 실패는 메인 프로세스에 영향을 주지 않음
        }
    }

    /**
     * 배치로 카프카 메시지 전송 (비동기 처리 및 완료 대기)
     */
    private fun sendBatchMessages(topic: String, messages: List<ContentProcessedMessage>) {
        val futures = messages.map { message ->
            kafkaTemplate.send(topic, message)
        }.toTypedArray()

        try {
            // 타임아웃 설정 (예: 30초)
            CompletableFuture.allOf(*futures).get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            logger.error("배치 전송 타임아웃: ${messages.size}개 중 일부 실패 가능")
            // TODO: 처리로직 추가
            throw e
        } catch (e: ExecutionException) {
            logger.error("배치 전송 실패", e.cause)
            // TODO: 처리로직 추가
            throw e
        }

    }
}

/**
 * 카프카로 전송할 메시지 데이터 클래스
 */
data class ContentProcessedMessage(
    val id: String,
    val type: SourceType,
    val processedAt: LocalDateTime,
    val content: Map<String, Any>
) {
    companion object {
        fun from(sourceContent: SourceContent): ContentProcessedMessage {
            return ContentProcessedMessage(
                id = requireNotNull(sourceContent.id) { "SourceContent must have id after saving" },
                type = sourceContent.type,
                processedAt = sourceContent.processedAt,
                content = sourceContent.content
            )
        }
    }
}