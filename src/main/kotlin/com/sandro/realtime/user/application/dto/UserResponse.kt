package com.sandro.realtime.user.application.dto

import com.sandro.realtime.user.domain.model.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사용자 응답")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,
    @Schema(description = "이메일 주소", example = "hong@example.com")
    val email: String,
    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                id = user.id!!,
                name = user.name,
                email = user.email,
                createdAt = user.createdAt!!,
                updatedAt = user.updatedAt!!,
            )
    }
}
