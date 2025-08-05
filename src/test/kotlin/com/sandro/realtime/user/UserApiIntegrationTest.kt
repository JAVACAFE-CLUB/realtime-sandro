package com.sandro.realtime.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.giveMeBuilder
import com.navercorp.fixturemonkey.kotlin.setExp
import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import com.sandro.realtime.user.domain.repository.UserRepository
import io.kotest.inspectors.forSingle
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.transaction.Transactional
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

    private val fixtureMonkey = FixtureMonkey.builder()
        .plugin(com.navercorp.fixturemonkey.kotlin.KotlinPlugin())
        .plugin(com.navercorp.fixturemonkey.jackson.plugin.JacksonPlugin())
        .build()

    @Test
    @DisplayName("유저 생성 - 정상적으로 유저가 생성되어야 한다")
    fun `should create user successfully`() {
        // given
        val request = createValidUserRequest()

        // when & then
        mockMvc
            .post("/api/v1/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andDo { print() }
            .andExpect { status { isCreated() } }
            .andExpect { jsonPath("$.name") { value(request.name) } }
            .andExpect { jsonPath("$.email") { value(request.email) } }
            .andExpect { jsonPath("$.id") { exists() } }
            .andExpect { jsonPath("$.createdAt") { exists() } }
            .andExpect { jsonPath("$.updatedAt") { exists() } }

        userRepository.findAll().forSingle { user ->
            user.id shouldNotBe null
            user.name shouldBe request.name
            user.email shouldBe request.email
            user.createdAt shouldNotBe null
            user.updatedAt shouldNotBe null
        }
    }

    @Test
    @DisplayName("유저 생성 - 중복된 이메일로 생성 시 409 에러가 발생해야 한다")
    fun `should fail to create user when email already exists`() {
        // given
        val existingUserRequest = createValidUserRequest()
        val existingUser = userRepository.save(existingUserRequest.toEntity())
        val duplicateRequest = fixtureMonkey.giveMeBuilder<UserCreateRequest>()
            .setExp(UserCreateRequest::name, "김철수")
            .setExp(UserCreateRequest::email, existingUser.email)
            .sample()

        // when & then
        mockMvc
            .post("/api/v1/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(duplicateRequest)
            }.andDo { print() }
            .andExpect { status().isConflict }
            .andExpect { jsonPath("$.message") { value("User already exists with email: ${existingUser.email}") } }
    }

    @Test
    @DisplayName("유저 조회 - 전체 유저 목록을 정상적으로 조회해야 한다")
    fun `should get all users successfully`() {
        // given
        val userRequests = listOf(
            createValidUserRequest(),
            fixtureMonkey.giveMeBuilder<UserCreateRequest>()
                .setExp(UserCreateRequest::name, "김철수")
                .setExp(UserCreateRequest::email, "kim@example.com")
                .sample()
        )
        val users = userRequests.map { it.toEntity() }
        userRepository.saveAll(users)

        // when & then
        mockMvc
            .get("/api/v1/users")
            .andDo { print() }.andExpect { status().isOk }
            .andExpect { jsonPath("$.length()") { value(2) } }
            .andExpect { jsonPath("$[0].name") { value(userRequests[0].name) } }
            .andExpect { jsonPath("$[0].email") { value(userRequests[0].email) } }
            .andExpect { jsonPath("$[1].name") { value(userRequests[1].name) } }
            .andExpect { jsonPath("$[1].email") { value(userRequests[1].email) } }
    }

    @Test
    @DisplayName("유저 조회 - ID로 특정 유저를 정상적으로 조회해야 한다")
    fun `should get user by id successfully`() {
        // given
        val userRequest = createValidUserRequest()
        val userId = userRepository.save(userRequest.toEntity()).id

        // when & then
        mockMvc
            .get("/api/v1/users/$userId")
            .andDo { print() }.andExpect { status().isOk }
            .andExpect { jsonPath("$.id") { value(userId) } }
            .andExpect { jsonPath("$.name") { value(userRequest.name) } }
            .andExpect { jsonPath("$.email") { value(userRequest.email) } }
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
        val userRequest = createValidUserRequest()
        val savedUser = userRepository.save(userRequest.toEntity())
        val userId = savedUser.id
        val updateRequest = fixtureMonkey.giveMeBuilder<UserUpdateRequest>()
            .setExp(UserUpdateRequest::name, "홍길동수정")
            .sample()

        // when & then
        mockMvc
            .put("/api/v1/users/$userId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRequest)
            }.andDo { print() }
            .andExpect { status().isOk }
            .andExpect { jsonPath("$.id") { value(userId) } }
            .andExpect { jsonPath("$.name") { value(updateRequest.name) } }
            .andExpect { jsonPath("$.email") { value(userRequest.email) } }

        userRepository.findById(userId!!).shouldBePresent { user ->
            user.name shouldBe updateRequest.name
            user.email shouldBe userRequest.email
        }
    }

    @Test
    @DisplayName("유저 수정 - 존재하지 않는 유저 수정 시 404 에러가 발생해야 한다")
    fun `should fail to update user when user not found`() {
        // given
        val userId = 999
        val updateRequest = fixtureMonkey.giveMeBuilder<UserUpdateRequest>()
            .setExp(UserUpdateRequest::name, "홍길동")
            .sample()

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
        val userRequest = createValidUserRequest()
        val userId = userRepository.save(userRequest.toEntity()).id

        // when
        mockMvc
            .delete("/api/v1/users/$userId")
            .andDo { print() }
            .andExpect { status().isOk }

        // then
        userRepository.findById(userId!!).shouldNotBePresent()
    }

    private fun createValidUserRequest(): UserCreateRequest {
        val baseRequest = fixtureMonkey.giveMeBuilder<UserCreateRequest>().sample()
        val safeName = baseRequest.name.take(20).ifBlank { "TestUser" }
        return baseRequest.copy(
            name = safeName,
            email = "${safeName.lowercase().replace(" ", "")}@example.com"
        )
    }

}