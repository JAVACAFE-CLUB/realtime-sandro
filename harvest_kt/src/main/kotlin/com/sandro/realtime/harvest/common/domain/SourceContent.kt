package com.sandro.realtime.harvest.common.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sandro.realtime.harvest.news.domain.NewsArticle
import com.sandro.realtime.harvest.wiki.domain.WikiPage
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "sourceContents")
@CompoundIndexes(
    CompoundIndex(
        name = "wiki_page_unique_idx",
        def = "{'type': 1, 'content.id': 1}",
        unique = true,
        partialFilter = "{'type': 'WIKIPEDIA'}"
    ),
    CompoundIndex(
        name = "wiki_revision_query_idx",
        def = "{'type': 1, 'content.id': 1, 'content.revision.id': 1}",
        partialFilter = "{'type': 'WIKIPEDIA'}"
    ),
    CompoundIndex(
        name = "news_article_unique_idx",
        def = "{'type': 1, 'content.articleId': 1}",
        unique = true,
        partialFilter = "{'type': 'NEWS'}"
    )
)
data class SourceContent(
    @Id
    val id: String? = null,
    val type: SourceType,
    var processedAt: LocalDateTime = LocalDateTime.now(), // 처리일시
    var content: Map<String, Any>
) {
    companion object {
        private val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        /**
         * 범용적인 객체에서 SourceContent로 변환
         */
        fun <T> from(sourceType: SourceType, sourceObject: T): SourceContent {
            return SourceContent(
                type = sourceType,
                content = objectMapper.convertValue(sourceObject, Map::class.java) as Map<String, Any>
            )
        }

        fun from(article: NewsArticle): SourceContent {
            return from(SourceType.NEWS, article)
        }

        fun from(wikiPage: WikiPage): SourceContent {
            return from(SourceType.WIKIPEDIA, wikiPage)
        }
    }

    fun update(content: SourceContent) {
        this.content = content.content
        this.processedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceContent

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
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