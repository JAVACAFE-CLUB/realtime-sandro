package com.sandro.realtime.smithy.document.repository

import com.sandro.realtime.smithy.document.ExtractedContent
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * ExtractedContent MongoDB Repository
 *
 * 추출된 fulltext를 MongoDB에 저장하고 조회하는 Repository
 */
@Repository
interface ExtractedContentRepository : MongoRepository<ExtractedContent, String> {
    /**
     * sourceId로 추출된 콘텐츠 조회
     */
    fun findBySourceId(sourceId: String): ExtractedContent?

    /**
     * sourceId 존재 여부 확인
     */
    fun existsBySourceId(sourceId: String): Boolean
}
