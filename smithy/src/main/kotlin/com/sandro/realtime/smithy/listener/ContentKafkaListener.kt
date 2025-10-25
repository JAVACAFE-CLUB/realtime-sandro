package com.sandro.realtime.smithy.listener

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.message.ContentProcessedMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/**
 * harvest 모듈에서 발행한 콘텐츠 처리 완료 메시지를 수신하는 리스너
 *
 * 현재는 메시지를 수신하여 로깅만 수행하며, 향후 문서 파싱, MongoDB 저장 등의 기능을 추가할 수 있습니다.
 */
@Service
class ContentKafkaListener {

    private val logger = LoggerFactory.getLogger(ContentKafkaListener::class.java)

    /**
     * Wikipedia 콘텐츠 처리 완료 메시지 수신
     *
     * @param message harvest 모듈에서 발행한 Wikipedia 콘텐츠 메시지
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.WIKI_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleWikiContentProcessed(
        message: ContentProcessedMessage,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.debug("Wiki 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}, processedAt=${message.processedAt}")
            logger.debug("콘텐츠 상세: ${message.content}")

            // TODO: 향후 문서 파싱, MongoDB 저장 등의 처리 추가

            // 수동 커밋
            acknowledgment.acknowledge()
            logger.debug("Wiki 콘텐츠 메시지 처리 완료: ${message.id}")
        } catch (e: Exception) {
            logger.error("Wiki 콘텐츠 메시지 처리 실패: ${message.id}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }

    /**
     * News 콘텐츠 처리 완료 메시지 수신
     *
     * @param message harvest 모듈에서 발행한 News 콘텐츠 메시지
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.NEWS_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleNewsContentProcessed(
        message: ContentProcessedMessage,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.debug("News 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}, processedAt=${message.processedAt}")
            logger.debug("콘텐츠 상세: ${message.content}")

            // TODO: 향후 문서 파싱, MongoDB 저장 등의 처리 추가

            // 수동 커밋
            acknowledgment.acknowledge()
            logger.debug("News 콘텐츠 메시지 처리 완료: ${message.id}")
        } catch (e: Exception) {
            logger.error("News 콘텐츠 메시지 처리 실패: ${message.id}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }
}
