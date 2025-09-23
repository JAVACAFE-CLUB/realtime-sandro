package com.sandro.realtime.harvest.common.domain

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "sourceContents")
data class SourceContent(
    @Id
    val id: String? = null,
    val type: SourceType,
    val processedAt: LocalDateTime = LocalDateTime.now(), // 처리일시
    val content: Map<String, Any>
) {
    companion object {
        private val objectMapper = ObjectMapper()

        /**
         * 범용적인 객체에서 SourceContent로 변환
         */
        fun <T> from(sourceType: SourceType, sourceObject: T): SourceContent {
            return SourceContent(
                type = sourceType,
                content = objectMapper.convertValue(sourceObject, Map::class.java) as Map<String, Any>
            )
        }
    }
}

/**
 * 수집 가능한 소스 타입 정의
 */
enum class SourceType {
    WIKIPEDIA,      // 위키피디아
    NEWS,           // 뉴스 기사
    BLOG,           // 블로그 포스트
    SOCIAL,         // 소셜 미디어
}