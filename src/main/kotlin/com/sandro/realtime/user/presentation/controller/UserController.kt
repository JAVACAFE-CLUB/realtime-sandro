package com.sandro.realtime.user.presentation.controller

import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserResponse
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import com.sandro.realtime.user.application.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    fun createUser(
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request))

    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getAllUsers())

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.getUserById(id))

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateUser(id, request))

    @DeleteMapping("/{id}")
    fun deleteUser(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.ok().build()
    }
}
