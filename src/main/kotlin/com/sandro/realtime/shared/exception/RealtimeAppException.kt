package com.sandro.realtime.shared.exception

import org.springframework.http.HttpStatus

class RealtimeAppException(
    override val message: String,
    val httpStatus: HttpStatus
) : RuntimeException() {

    companion object {
        fun userNotFound(id: Long): RealtimeAppException {
            return RealtimeAppException(
                message = "User not found with id: $id",
                httpStatus = HttpStatus.NOT_FOUND
            )
        }

        fun userAlreadyExists(email: String): RealtimeAppException {
            return RealtimeAppException(
                message = "User already exists with email: $email",
                httpStatus = HttpStatus.CONFLICT
            )
        }
    }
}