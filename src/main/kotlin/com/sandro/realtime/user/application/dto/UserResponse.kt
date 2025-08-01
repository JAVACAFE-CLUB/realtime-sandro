package com.sandro.realtime.user.application.dto

import com.sandro.realtime.user.domain.model.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime,
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
