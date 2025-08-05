package com.sandro.realtime.user.application.dto

import com.sandro.realtime.user.domain.model.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "사용자 생성 요청")
data class UserCreateRequest(
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,
    @Schema(description = "이메일 주소", example = "hong@example.com", required = true)
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
) {
    fun toEntity(): User {
        return User(name = name, email = email)
    }
}