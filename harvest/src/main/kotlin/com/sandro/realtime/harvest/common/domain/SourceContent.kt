package com.sandro.realtime.harvest.common.domain

import com.sandro.realtime.common.domain.SourceType
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
        def = "{'type': 1, 'content.officeId': 1, 'content.articleId': 1}",
        unique = true,
        partialFilter = "{'type': 'NEWS'}"
    )
)
data class SourceContent(
    @Id
    val id: String? = null,
    val type: SourceType,
    var processedAt: LocalDateTime = LocalDateTime.now(), // 처리일시
    var content: Map<String, Any?>
) {
    fun update(content: SourceContent) {
        this.content = content.content
        this.processedAt = LocalDateTime.now()
    }

    fun getLastModifiedAt(): String? {
        return content["lastModifiedAt"] as? String
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