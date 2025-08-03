package com.sandro.realtime.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import com.sandro.realtime.user.domain.model.User
import com.sandro.realtime.user.domain.repository.UserRepository
import io.kotest.inspectors.forSingle
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class UserApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("유저 생성 - 정상적으로 유저가 생성되어야 한다")
    fun `should create user successfully`() {
        // given
        val request = UserCreateRequest(
            name = "홍길동",
            email = "hong@example.com",
        )

        // when & then
        mockMvc
            .post("/api/v1/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andDo { print() }
            .andExpect { status { isCreated() } }
            .andExpect { jsonPath("$.name") { value("홍길동") } }
            .andExpect { jsonPath("$.email") { value("hong@example.com") } }
            .andExpect { jsonPath("$.id") { exists() } }
            .andExpect { jsonPath("$.createdAt") { exists() } }
            .andExpect { jsonPath("$.updatedAt") { exists() } }

        userRepository.findAll().forSingle { user ->
            user.id shouldNotBe null
            user.name shouldBe "홍길동"
            user.email shouldBe "hong@example.com"
            user.createdAt shouldNotBe null
            user.updatedAt shouldNotBe null
        }
    }

    @Test
    @DisplayName("유저 생성 - 유효성 검사 실패 시 400 에러가 발생해야 한다")
    fun `should fail to create user when validation fails`() {
        // given
        val request = UserCreateRequest(
            name = "",
            email = "invalid-email",
        )

        // when & then
        mockMvc
            .post("/api/v1/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andDo { print() }
            .andExpect { status().isBadRequest }
            .andExpect { jsonPath("$.message", containsString("email: 올바른 이메일 형식이 아닙니다")) }
            .andExpect { jsonPath("$.message", containsString("name: 이름은 필수입니다")) }
    }

    @Test
    @DisplayName("유저 생성 - 중복된 이메일로 생성 시 409 에러가 발생해야 한다")
    fun `should fail to create user when email already exists`() {
        // given
        val email = "duplicate@example.com"
        userRepository.save(User(name = "홍길동", email = email))
        val secondRequest = UserCreateRequest("김철수", email)

        // when & then
        mockMvc
            .post("/api/v1/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(secondRequest)
            }.andDo { print() }
            .andExpect { status().isConflict }
            .andExpect { jsonPath("$.message") { value("User already exists with email: $email") } }
    }

    @Test
    @DisplayName("유저 조회 - 전체 유저 목록을 정상적으로 조회해야 한다")
    fun `should get all users successfully`() {
        // given
        userRepository.saveAll(
            listOf(
                User("홍길동", "hong@example.com"),
                User("김철수", "kim@example.com")
            )
        )

        // when & then
        mockMvc
            .get("/api/v1/users")
            .andDo { print() }.andExpect { status().isOk }
            .andExpect { jsonPath("$.length()") { value(2) } }
            .andExpect { jsonPath("$[0].name") { value("홍길동") } }
            .andExpect { jsonPath("$[0].email") { value("hong@example.com") } }
            .andExpect { jsonPath("$[1].name") { value("김철수") } }
            .andExpect { jsonPath("$[1].email") { value("kim@example.com") } }
    }

    @Test
    @DisplayName("유저 조회 - ID로 특정 유저를 정상적으로 조회해야 한다")
    fun `should get user by id successfully`() {
        // given
        val userId = userRepository.save(User("홍길동", "hong@example.com")).id

        // when & then
        mockMvc
            .get("/api/v1/users/$userId")
            .andDo { print() }.andExpect { status().isOk }
            .andExpect { jsonPath("$.id") { value(userId) } }
            .andExpect { jsonPath("$.name") { value("홍길동") } }
            .andExpect { jsonPath("$.email") { value("hong@example.com") } }
    }

    @Test
    @DisplayName("유저 조회 - 존재하지 않는 유저 조회 시 404 에러가 발생해야 한다")
    fun `should fail to get user when user not found`() {
        // given
        val userId = 999

        // when & then
        mockMvc
            .get("/api/v1/users/$userId")
            .andDo { print() }
            .andExpect { status().isNotFound }
            .andExpect { jsonPath("$.message") { value("User not found with id: $userId") } }
    }

    @Test
    @DisplayName("유저 수정 - 유저 정보를 정상적으로 수정해야 한다")
    fun `should update user successfully`() {
        // given
        val userId = userRepository.save(User("홍길동", "hong@example.com")).id
        val updateRequest = UserUpdateRequest("홍길동수정")

        // when & then
        mockMvc
            .put("/api/v1/users/$userId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRequest)
            }.andDo { print() }
            .andExpect { status().isOk }
            .andExpect { jsonPath("$.id") { value(userId) } }
            .andExpect { jsonPath("$.name") { value("홍길동수정") } }
            .andExpect { jsonPath("$.email") { value("hong@example.com") } }

        userRepository.findById(userId!!).shouldBePresent { user ->
            user.name shouldBe "홍길동수정"
            user.email shouldBe "hong@example.com"
        }
    }

    @Test
    @DisplayName("유저 수정 - 존재하지 않는 유저 수정 시 404 에러가 발생해야 한다")
    fun `should fail to update user when user not found`() {
        // given
        val userId = 999
        val updateRequest = UserUpdateRequest("홍길동")

        // when & then
        mockMvc
            .put("/api/v1/users/$userId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRequest)
            }.andDo { print() }
            .andExpect { status().isNotFound }
            .andExpect { jsonPath("$.message") { value("User not found with id: $userId") } }
    }

    @Test
    @DisplayName("유저 삭제 - 유저를 정상적으로 삭제해야 한다")
    fun `should delete user successfully`() {
        // given
        val userId = userRepository.save(User("홍길동", "hong@example.com")).id

        // when
        mockMvc
            .delete("/api/v1/users/$userId")
            .andDo { print() }
            .andExpect { status().isOk }

        // then
        userRepository.findById(userId!!).shouldNotBePresent()
    }
}