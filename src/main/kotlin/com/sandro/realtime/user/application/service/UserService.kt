package com.sandro.realtime.user.application.service

import com.sandro.realtime.shared.exception.RealtimeAppException
import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserResponse
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import com.sandro.realtime.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createUser(request: UserCreateRequest): UserResponse {
        val savedUser = userRepository.save(request.toEntity())
        return UserResponse.from(savedUser)
    }

    fun getAllUsers(): List<UserResponse> = userRepository.findAll().map { UserResponse.from(it) }

    fun getUserById(id: Long): UserResponse {
        return userRepository.findById(id)
            .map { UserResponse.from(it) }
            .orElseThrow { RealtimeAppException.userNotFound(id) }
    }

    @Transactional
    fun updateUser(id: Long, request: UserUpdateRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow { RealtimeAppException.userNotFound(id) }

        user.update(request.name)

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id))
            throw RealtimeAppException.userNotFound(id)
        userRepository.deleteById(id)
    }
}