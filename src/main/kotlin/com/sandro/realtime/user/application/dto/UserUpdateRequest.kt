package com.sandro.realtime.user.application.dto

import jakarta.validation.constraints.NotBlank

data class UserUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String
)