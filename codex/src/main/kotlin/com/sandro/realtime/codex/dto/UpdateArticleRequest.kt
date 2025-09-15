package com.sandro.realtime.codex.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 수정 요청")
data class UpdateArticleRequest(
    @field:Schema(description = "게시글 제목 (선택적)", example = "수정된 제목")
    val title: String? = null,

    @field:Schema(description = "게시글 내용 (선택적)", example = "수정된 내용입니다.")
    val content: String? = null
)