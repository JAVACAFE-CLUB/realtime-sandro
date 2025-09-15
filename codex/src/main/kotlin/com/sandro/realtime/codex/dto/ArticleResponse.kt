package com.sandro.realtime.codex.dto

import com.sandro.realtime.codex.domain.Article
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "게시글 응답")
data class ArticleResponse(
    @Schema(description = "게시글 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,

    @Schema(description = "게시글 제목", example = "스프링 부트와 Elasticsearch 연동하기")
    val title: String,

    @Schema(description = "게시글 내용", example = "이번 포스팅에서는 스프링 부트와 Elasticsearch를 연동하는 방법을 알아보겠습니다.")
    val content: String,

    @Schema(description = "생성일시", example = "2024-09-15T10:30:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정일시", example = "2024-09-15T10:30:00")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(article: Article): ArticleResponse {
            return ArticleResponse(
                id = article.id ?: "",
                title = article.title,
                content = article.content,
                createdAt = article.createdAt,
                updatedAt = article.updatedAt
            )
        }
    }
}