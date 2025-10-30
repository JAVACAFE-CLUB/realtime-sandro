package com.sandro.realtime.common.message

import com.sandro.realtime.common.domain.SourceType
import java.time.LocalDateTime

/**
 * smithy 모듈에서 fulltext 추출 완료 시 발행하는 메시지
 *
 * harvest 모듈에서 수집한 콘텐츠를 TikaDocumentParser로 파싱하여
 * 추출된 텍스트를 codex 모듈로 전달하기 위한 메시지
 */
data class TextExtractedMessage(
    val sourceId: String,           // 원본 SourceContent ID
    val sourceType: SourceType,     // WIKI, NEWS 등
    val extractedText: String,      // 추출된 fulltext
    val extractedAt: LocalDateTime  // 추출 시간
)
