package com.sandro.realtime.harvest.wiki.event

import com.sandro.realtime.harvest.common.domain.SourceContent
import org.springframework.context.ApplicationEvent

/**
 * WikiPage 배치 처리 완료 이벤트
 */
data class WikiPagesBatchProcessedEvent(
    val pages: List<SourceContent>
) : ApplicationEvent(pages)