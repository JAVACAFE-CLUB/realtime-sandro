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
     * Wikipedia 콘텐츠 처리 완료 메시지 배치 수신
     *
     * @param messageJsonList harvest 모듈에서 발행한 Wikipedia 콘텐츠 메시지 목록 (JSON String List)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.WIKI_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"
    )
    fun handleWikiContentProcessed(
        messageJsonList: List<String>,
        acknowledgment: Acknowledgment
    ) {
        logger.info("Wiki 콘텐츠 배치 수신: 배치 크기=${messageJsonList.size}")

        var successCount = 0
        var failCount = 0

        messageJsonList.forEach { messageJson ->
            try {
                // TODO: 메시지 별로 특화된 모델 사용하기
                // JSON String을 ContentProcessedMessage로 역직렬화
                val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

                logger.debug("Wiki 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}")

                // 서비스에 처리 위임
                contentProcessingService.processWikiContent(message)

                successCount++
                logger.debug("Wiki 콘텐츠 처리 완료: id=${message.id}")
            } catch (e: IllegalArgumentException) {
                failCount++
                logger.error("Wiki 콘텐츠 메시지 형식 오류: error=${e.message}", e)
                // TODO: DLQ(Dead Letter Queue)로 전송하여 별도 처리 필요
            } catch (e: Exception) {
                failCount++
                logger.error("Wiki 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
                // TODO: 재처리 정책 수립 필요 (현재는 배치 전체 재처리)
            }
        }

        // 배치 단위 커밋
        acknowledgment.acknowledge()
        logger.info("Wiki 콘텐츠 배치 처리 완료: 성공=$successCount, 실패=$failCount, 전체=${messageJsonList.size}")
    }

    /**
     * News 콘텐츠 처리 완료 메시지 배치 수신
     *
     * @param messageJsonList harvest 모듈에서 발행한 News 콘텐츠 메시지 목록 (JSON String List)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.NEWS_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"
    )
    fun handleNewsContentProcessed(
        messageJsonList: List<String>,
        acknowledgment: Acknowledgment
    ) {
        logger.info("News 콘텐츠 배치 수신: 배치 크기=${messageJsonList.size}")

        var successCount = 0
        var failCount = 0

        messageJsonList.forEach { messageJson ->
            try {
                // JSON String을 ContentProcessedMessage로 역직렬화
                val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

                logger.debug("News 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}")

                // 서비스에 처리 위임
                contentProcessingService.processNewsContent(message)

                successCount++
                logger.debug("News 콘텐츠 처리 완료: id=${message.id}")
            } catch (e: IllegalArgumentException) {
                failCount++
                logger.error("News 콘텐츠 메시지 형식 오류: error=${e.message}", e)
                // TODO: DLQ(Dead Letter Queue)로 전송하여 별도 처리 필요
            } catch (e: Exception) {
                failCount++
                logger.error("News 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
                // TODO: 재처리 정책 수립 필요 (현재는 배치 전체 재처리)
            }
        }

        // 배치 단위 커밋
        acknowledgment.acknowledge()
        logger.info("News 콘텐츠 배치 처리 완료: 성공=$successCount, 실패=$failCount, 전체=${messageJsonList.size}")
    }
}
