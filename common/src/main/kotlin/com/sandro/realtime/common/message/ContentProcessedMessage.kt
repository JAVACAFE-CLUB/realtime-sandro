package com.sandro.realtime.common.message

import com.sandro.realtime.common.domain.SourceType
import java.time.LocalDateTime

/**
 * 카프카로 전송할 메시지 데이터 클래스
 *
 * harvest 모듈에서 수집한 콘텐츠 처리 완료 시 발행하는 메시지
 */
data class ContentProcessedMessage(
    val id: String,
    val type: SourceType,
    val processedAt: LocalDateTime,
    val content: Map<String, Any?>
)
