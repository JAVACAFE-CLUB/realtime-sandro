package com.sandro.realtime.codex.dto

import com.sandro.realtime.codex.domain.Document
import com.sandro.realtime.codex.domain.DocumentType
import java.time.LocalDateTime

data class DocumentResponse(
    val id: String?,
    val documentType: DocumentType,
    val title: String?,
    val content: String,
    val source: String?,
    val publishedAt: LocalDateTime?,
    val author: String?,
    val categories: List<String>,
    val tags: List<String>,
    val url: String?,
    val metadata: Map<String, Any>,
    val indexedAt: LocalDateTime
) {
    companion object {
        fun from(document: Document): DocumentResponse {
            return DocumentResponse(
                id = document.id,
                documentType = document.documentType,
                title = document.title,
                content = document.content,
                source = document.source,
                publishedAt = document.publishedAt,
                author = document.author,
                categories = document.categories,
                tags = document.tags,
                url = document.url,
                metadata = document.metadata,
                indexedAt = document.indexedAt
            )
        }
    }
}