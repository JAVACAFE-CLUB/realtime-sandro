package com.sandro.realtime.exception

class UserNotFoundException(
    id: Long,
) : RuntimeException("User not found with id: $id")
