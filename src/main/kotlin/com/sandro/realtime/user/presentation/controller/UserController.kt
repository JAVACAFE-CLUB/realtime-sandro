package com.sandro.realtime.user.presentation.controller

import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserResponse
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import com.sandro.realtime.user.application.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "User API", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)"),
            ApiResponse(responseCode = "409", description = "이메일 중복")
        ]
    )
    @PostMapping
    fun createUser(
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request))

    @Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getAllUsers())

    @Operation(summary = "사용자 조회", description = "ID로 특정 사용자를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    @GetMapping("/{id}")
    fun getUserById(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable id: Long,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.getUserById(id))

    @Operation(summary = "사용자 수정", description = "ID로 특정 사용자의 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    @PutMapping("/{id}")
    fun updateUser(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateUser(id, request))

    @Operation(summary = "사용자 삭제", description = "ID로 특정 사용자를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    @DeleteMapping("/{id}")
    fun deleteUser(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.ok().build()
    }
}
