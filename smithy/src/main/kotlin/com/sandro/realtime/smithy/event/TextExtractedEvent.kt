package com.sandro.realtime.smithy.event

import com.sandro.realtime.common.message.TextExtractedMessage
import org.springframework.context.ApplicationEvent

/**
 * 텍스트 추출 완료 이벤트
 *
 * 콘텐츠에서 fulltext가 추출되고 MongoDB에 저장된 후 발행되는 이벤트입니다.
 * 이 이벤트를 수신한 리스너가 Kafka로 메시지를 발송합니다.
 *
 * @property message 추출된 텍스트 정보를 담은 메시지
 */
class TextExtractedEvent(
    source: Any,
    val message: TextExtractedMessage
) : ApplicationEvent(source)
