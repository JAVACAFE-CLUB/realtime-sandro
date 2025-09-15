package com.sandro.realtime.codex.dto

import com.sandro.realtime.codex.domain.DocumentType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class DocumentSearchRequest(
    @field:NotBlank(message = "검색어는 필수입니다")
    val query: String,

    val documentType: DocumentType? = null,

    val category: String? = null,

    val source: String? = null,

    val author: String? = null,

    val tag: String? = null,

    @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    val page: Int = 0,

    @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    val size: Int = 20,

    val sortBy: String = "indexedAt",

    val sortDirection: String = "DESC"
)