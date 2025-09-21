package com.sandro.realtime.harvest.event

import com.sandro.realtime.harvest.domain.SourceContent
import org.springframework.context.ApplicationEvent

/**
 * WikiPage 배치 처리 완료 이벤트
 */
data class WikiPagesBatchProcessedEvent(
    val pages: List<SourceContent>
) : ApplicationEvent(pages)

/**
 * NewsArticle 배치 처리 완료 이벤트
 */
data class NewsArticlesBatchProcessedEvent(
    val pages: List<SourceContent>
) : ApplicationEvent(pages)