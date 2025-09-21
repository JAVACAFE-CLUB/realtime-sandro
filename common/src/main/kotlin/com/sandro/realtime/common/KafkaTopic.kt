package com.sandro.realtime.common

/**
 * Kafka 토픽 상수 정의
 *
 * 네이밍 컨벤션: {source}.{domain}.{action}.{version}
 * - source: 메시지를 발행하는 서비스명
 * - domain: 비즈니스 도메인 (wiki, news, content 등)
 * - action: 이벤트 타입 (processed, indexed, requested 등)
 * - version: 스키마 버전 (v1, v2 등)
 */
object KafkaTopic {

    // ========== Harvest Service Topics ==========
    /**
     * 위키 콘텐츠 처리 완료 이벤트
     * Producer: harvest / Consumer: codex, smithy
     */
    const val WIKI_CONTENT_PROCESSED = "harvest.wiki.processed.v1"

    /**
     * 뉴스 콘텐츠 처리 완료 이벤트
     * Producer: harvest / Consumer: codex, smithy
     */
    const val NEWS_CONTENT_PROCESSED = "harvest.news.processed.v1"

    // ========== Codex Service Topics ==========
    /**
     * 콘텐츠 인덱싱 완료 이벤트
     * Producer: codex / Consumer: portal
     */
    const val CONTENT_INDEXED = "codex.content.indexed.v1"

    /**
     * 인덱스 업데이트 요청 이벤트
     * Producer: portal, harvest / Consumer: codex
     */
    const val INDEX_UPDATE_REQUESTED = "codex.index.update-requested.v1"

    // ========== Portal Service Topics ==========
    /**
     * 콘텐츠 조회 이벤트 (분석용)
     * Producer: portal / Consumer: analytics
     */
    const val CONTENT_VIEWED = "portal.content.viewed.v1"

    /**
     * 검색 요청 이벤트 (분석용)
     * Producer: portal / Consumer: analytics
     */
    const val SEARCH_REQUESTED = "portal.search.requested.v1"

    // ========== Smithy Service Topics ==========
    /**
     * AI 처리 완료 이벤트
     * Producer: smithy / Consumer: codex, portal
     */
    const val AI_PROCESSING_COMPLETED = "smithy.ai.completed.v1"

    /**
     * AI 처리 요청 이벤트
     * Producer: portal, harvest / Consumer: smithy
     */
    const val AI_PROCESSING_REQUESTED = "smithy.ai.requested.v1"

    // ========== System Topics ==========
    /**
     * 시스템 에러 이벤트 (모니터링용)
     * Producer: all services / Consumer: monitoring
     */
    const val SYSTEM_ERROR = "system.error.occurred.v1"

    /**
     * 데드레터 큐 (처리 실패한 메시지)
     * Producer: all services / Consumer: monitoring, retry-handler
     */
    const val DEAD_LETTER_QUEUE = "system.dlq.v1"
}