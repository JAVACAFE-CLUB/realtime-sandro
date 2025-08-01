package com.sandro.realtime.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.sandro.realtime.user.application.dto.UserCreateRequest
import com.sandro.realtime.user.application.dto.UserUpdateRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class UserApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create user successfully`() {
        val request =
            UserCreateRequest(
                name = "홍길동",
                email = "hong@example.com",
            )

        mockMvc
            .perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `should fail to create user when validation fails`() {
        val request =
            UserCreateRequest(
                name = "",
                email = "invalid-email",
            )

        mockMvc
            .perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `should fail to create user when email already exists`() {
        // 첫 번째 유저 생성
        val firstRequest = UserCreateRequest("홍길동", "duplicate@example.com")
        mockMvc.perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)),
        ).andExpect(status().isCreated)

        // 같은 이메일로 두 번째 유저 생성 시도
        val secondRequest = UserCreateRequest("김철수", "duplicate@example.com")
        mockMvc
            .perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
    }

    @Test
    fun `should get all users successfully`() {
        val request1 = UserCreateRequest("홍길동", "hong@example.com")
        val request2 = UserCreateRequest("김철수", "kim@example.com")

        mockMvc.perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        )
        mockMvc.perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        )

        mockMvc
            .perform(get("/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("홍길동"))
            .andExpect(jsonPath("$[1].name").value("김철수"))
    }

    @Test
    fun `should get user by id successfully`() {
        val request = UserCreateRequest("홍길동", "hong@example.com")

        val result =
            mockMvc
                .perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        val userId = response.get("id").asLong()

        mockMvc
            .perform(get("/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
    }

    @Test
    fun `should fail to get user when user not found`() {
        mockMvc
            .perform(get("/users/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should update user successfully`() {
        val createRequest = UserCreateRequest("홍길동", "hong@example.com")

        val result =
            mockMvc
                .perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        val userId = response.get("id").asLong()

        val updateRequest = UserUpdateRequest("홍길동수정")

        mockMvc
            .perform(
                put("/users/$userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value("홍길동수정"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
    }

    @Test
    fun `should fail to update user when user not found`() {
        val updateRequest = UserUpdateRequest("홍길동")

        mockMvc
            .perform(
                put("/users/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should delete user successfully`() {
        val request = UserCreateRequest("홍길동", "hong@example.com")

        val result =
            mockMvc
                .perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        val userId = response.get("id").asLong()

        mockMvc
            .perform(delete("/users/$userId"))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/users/$userId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should fail to delete user when user not found`() {
        mockMvc
            .perform(delete("/users/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }
}