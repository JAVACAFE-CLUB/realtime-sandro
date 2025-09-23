package com.sandro.realtime.harvest.event

import com.sandro.realtime.harvest.common.domain.SourceContent
import org.springframework.context.ApplicationEvent

/**
 * NewsArticle 배치 처리 완료 이벤트
 */
data class NewsArticlesBatchProcessedEvent(
    val pages: List<SourceContent>
) : ApplicationEvent(pages)