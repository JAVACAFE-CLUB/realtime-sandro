package com.sandro.realtime.smithy.document

import com.sandro.realtime.common.domain.SourceType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 추출된 fulltext를 저장하는 MongoDB Document
 *
 * harvest 모듈에서 수집한 원본 콘텐츠를 TikaDocumentParser로 파싱하여
 * 추출한 텍스트와 메타데이터를 저장합니다.
 */
@Document(collection = "extracted_contents")
data class ExtractedContent(
    @Id
    val id: String? = null,

    /**
     * 원본 콘텐츠 ID (harvest 모듈의 SourceContent ID)
     */
    val sourceId: String,

    /**
     * 콘텐츠 소스 타입 (WIKI, NEWS 등)
     */
    val sourceType: SourceType,

    /**
     * Tika로 추출한 fulltext
     */
    val extractedText: String,

    /**
     * 텍스트 추출 시간
     */
    val extractedAt: LocalDateTime,

    /**
     * 원본 텍스트 길이 (추출 전)
     */
    val originalLength: Int,

    /**
     * Tika가 감지한 언어 코드 (예: ko, en)
     */
    val detectedLanguage: String?
)
