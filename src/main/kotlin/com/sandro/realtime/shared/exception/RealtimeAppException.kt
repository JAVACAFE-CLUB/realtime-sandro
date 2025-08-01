package com.sandro.realtime.shared.exception

class RealtimeAppException(
    val errorCode: ErrorCode,
    val details: String? = null,
) : RuntimeException(details ?: errorCode.message) {

    companion object {
        fun userNotFound(id: Long): RealtimeAppException {
            return RealtimeAppException(
                errorCode = ErrorCode.USER_NOT_FOUND,
                details = "User not found with id: $id"
            )
        }

        fun userAlreadyExists(email: String): RealtimeAppException {
            return RealtimeAppException(
                errorCode = ErrorCode.USER_ALREADY_EXISTS,
                details = "User already exists with email: $email"
            )
        }
    }
}