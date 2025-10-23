package com.sandro.realtime.harvest.common.repository

import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SourceContentRepository : MongoRepository<SourceContent, String> {
    @Query("{ 'type': ?0, 'content.articleId': ?1 }")
    fun findByTypeAndContentArticleId(type: SourceType, articleId: String): Optional<SourceContent>
}