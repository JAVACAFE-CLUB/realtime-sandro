package com.sandro.realtime.harvest.news.event

import com.sandro.realtime.harvest.common.domain.SourceContent
import org.springframework.context.ApplicationEvent

/**
 * 개별 뉴스 기사 처리 완료 이벤트
 *
 * 기존의 NewsArticlesBatchProcessedEvent와 구분하여
 * 실시간 개별 뉴스 처리를 위한 이벤트입니다.
 */
data class NewsArticleProcessedEvent(
    val sourceContent: SourceContent
) : ApplicationEvent(sourceContent)