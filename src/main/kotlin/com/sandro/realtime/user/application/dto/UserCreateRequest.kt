package com.sandro.realtime.user.application.dto

import com.sandro.realtime.user.domain.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserCreateRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
) {
    fun toEntity(): User {
        return User(name = name, email = email)
    }
}