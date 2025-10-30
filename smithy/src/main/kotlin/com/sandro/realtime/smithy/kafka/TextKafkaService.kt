package com.sandro.realtime.smithy.kafka

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.message.TextExtractedMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/**
 * 텍스트 추출 결과를 Kafka로 발행하는 서비스
 */
@Service
class TextKafkaService(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(TextKafkaService::class.java)

    /**
     * 텍스트 추출 완료 메시지를 Kafka로 발송
     *
     * @param message 추출된 텍스트 정보를 담은 메시지
     */
    fun sendTextExtracted(message: TextExtractedMessage) {
        kafkaTemplate.send(KafkaTopic.TEXT_EXTRACTED, message)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("텍스트 추출 메시지 전송 실패: sourceId=${message.sourceId}, error=${ex.message}", ex)
                } else {
                    logger.info("텍스트 추출 메시지 전송 성공: sourceId=${message.sourceId}, type=${message.sourceType}")
                }
            }
    }
}
