package com.sandro.realtime.codex.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "게시글 생성 요청")
data class CreateArticleRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Schema(description = "게시글 제목", example = "스프링 부트와 Elasticsearch 연동하기")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @field:Schema(description = "게시글 내용", example = "이번 포스팅에서는 스프링 부트와 Elasticsearch를 연동하는 방법을 알아보겠습니다.")
    val content: String
)