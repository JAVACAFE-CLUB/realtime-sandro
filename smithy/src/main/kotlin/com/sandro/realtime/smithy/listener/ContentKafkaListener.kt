package com.sandro.realtime.smithy.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.smithy.service.ContentProcessingService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/**
 * harvest 모듈에서 발행한 콘텐츠 처리 완료 메시지를 수신하는 리스너
 */
@Service
class ContentKafkaListener(
    private val contentProcessingService: ContentProcessingService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(ContentKafkaListener::class.java)

    /**
     * Wikipedia 콘텐츠 처리 완료 메시지 수신
     *
     * @param messageJson harvest 모듈에서 발행한 Wikipedia 콘텐츠 메시지 (JSON String)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.WIKI_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"
    )
    fun handleWikiContentProcessed(
        messageJson: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            // TODO: 메시지 별로 특화된 모델 사용하기
            // JSON String을 ContentProcessedMessage로 역직렬화
            val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

            logger.info("Wiki 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}")

            // 서비스에 처리 위임
            contentProcessingService.processWikiContent(message)

            // 수동 커밋
            acknowledgment.acknowledge()
            logger.debug("Wiki 콘텐츠 처리 완료 및 커밋: id=${message.id}")
        } catch (e: IllegalArgumentException) {
            logger.error("Wiki 콘텐츠 메시지 형식 오류: error=${e.message}", e)
            // 메시지 형식 오류는 재처리해도 성공할 수 없으므로 커밋하여 skip
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Wiki 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }

    /**
     * News 콘텐츠 처리 완료 메시지 수신
     *
     * @param messageJson harvest 모듈에서 발행한 News 콘텐츠 메시지 (JSON String)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.NEWS_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"
    )
    fun handleNewsContentProcessed(
        messageJson: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            // JSON String을 ContentProcessedMessage로 역직렬화
            val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

            logger.info("News 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}")

            // 서비스에 처리 위임
            contentProcessingService.processNewsContent(message)

            // 수동 커밋
            acknowledgment.acknowledge()
            logger.debug("News 콘텐츠 처리 완료 및 커밋: id=${message.id}")
        } catch (e: IllegalArgumentException) {
            logger.error("News 콘텐츠 메시지 형식 오류: error=${e.message}", e)
            // 메시지 형식 오류는 재처리해도 성공할 수 없으므로 커밋하여 skip
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("News 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }
}
