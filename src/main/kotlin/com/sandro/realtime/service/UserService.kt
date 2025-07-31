package com.sandro.realtime.service

import com.sandro.realtime.domain.User
import com.sandro.realtime.dto.UserCreateRequest
import com.sandro.realtime.dto.UserResponse
import com.sandro.realtime.dto.UserUpdateRequest
import com.sandro.realtime.exception.UserNotFoundException
import com.sandro.realtime.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createUser(request: UserCreateRequest): UserResponse {
        val user = User(name = request.name, email = request.email)
        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    fun getAllUsers(): List<UserResponse> =
        userRepository
            .findAll()
            .map { UserResponse.from(it) }

    fun getUserById(id: Long): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { UserNotFoundException(id) }
        return UserResponse.from(user)
    }

    @Transactional
    fun updateUser(
        id: Long,
        request: UserUpdateRequest,
    ): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { UserNotFoundException(id) }

        user.name = request.name
        user.email = request.email

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }
        userRepository.deleteById(id)
    }
}
