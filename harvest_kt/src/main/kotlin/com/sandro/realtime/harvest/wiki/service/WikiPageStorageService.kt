package com.sandro.realtime.harvest.wiki.service

import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.wiki.domain.WikiPage
import com.sandro.realtime.harvest.wiki.event.WikiPagesBatchProcessedEvent
import com.sandro.realtime.harvest.wiki.repository.WikiPageRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * WikiPage 저장 서비스
 * 비즈니스 로직과 이벤트 처리를 담당
 */
@Service
class WikiPageStorageService(
    private val wikiPageRepository: WikiPageRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    /**
     * WikiPage 리스트를 MongoDB에 저장
     * MongoDB Bulk Operations를 통한 고성능 revision 조건부 upsert
     *
     * @param wikiPages 저장할 WikiPage 리스트
     * @return 새로 생성되거나 업데이트된 SourceContent 리스트
     */
    fun storeWikiPages(wikiPages: List<WikiPage>): List<SourceContent> {
        if (wikiPages.isEmpty()) return emptyList()

        // MongoDB Bulk Operations를 통한 배치 upsert
        val changedContents = wikiPageRepository.bulkUpsertWithRevisionCheck(wikiPages)

        // 이벤트 발행 (생성되거나 업데이트된 것들)
        if (changedContents.isNotEmpty()) {
            applicationEventPublisher.publishEvent(
                WikiPagesBatchProcessedEvent(changedContents)
            )
        }

        return changedContents
    }
}