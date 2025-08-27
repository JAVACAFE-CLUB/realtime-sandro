package com.sandro.realtime.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "사용자 수정 요청")
data class UserUpdateRequest(
    @Schema(description = "사용자 이름", example = "김철수", required = true)
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String
)