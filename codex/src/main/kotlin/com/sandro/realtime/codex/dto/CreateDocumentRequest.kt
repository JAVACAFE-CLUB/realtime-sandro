package com.sandro.realtime.codex.dto

import com.sandro.realtime.codex.domain.DocumentType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateDocumentRequest(
    @field:NotNull(message = "문서 타입은 필수입니다")
    val documentType: DocumentType,

    @field:Size(max = 500, message = "제목은 500자를 초과할 수 없습니다")
    val title: String? = null,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    val source: String? = null,

    val publishedAt: LocalDateTime? = null,

    val author: String? = null,

    val categories: List<String> = emptyList(),

    val tags: List<String> = emptyList(),

    val url: String? = null,

    val metadata: Map<String, Any> = emptyMap()
)