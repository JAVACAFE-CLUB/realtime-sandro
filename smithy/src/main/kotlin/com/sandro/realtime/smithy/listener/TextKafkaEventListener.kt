package com.sandro.realtime.smithy.listener

import com.sandro.realtime.smithy.event.TextExtractedEvent
import com.sandro.realtime.smithy.kafka.TextKafkaService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 텍스트 추출 이벤트 리스너
 *
 * TextExtractedEvent를 수신하여 Kafka로 메시지를 발송합니다.
 * ContentProcessingService와 Kafka 발송 로직 사이의 결합도를 낮추기 위해
 * ApplicationEvent 패턴을 사용합니다.
 */
@Component
class TextKafkaEventListener(
    private val textKafkaService: TextKafkaService
) {

    private val logger = LoggerFactory.getLogger(TextKafkaEventListener::class.java)

    /**
     * 텍스트 추출 완료 이벤트 처리
     *
     * @param event 텍스트 추출 완료 이벤트
     */
    @EventListener
    fun handleTextExtracted(event: TextExtractedEvent) {
        logger.info("텍스트 추출 이벤트 수신: sourceId=${event.message.sourceId}")

        textKafkaService.sendTextExtracted(event.message)

        logger.info("Kafka 메시지 발송 완료: sourceId=${event.message.sourceId}")
    }
}
