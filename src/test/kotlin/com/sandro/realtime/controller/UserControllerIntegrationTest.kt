package com.sandro.realtime.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.sandro.realtime.dto.UserCreateRequest
import com.sandro.realtime.dto.UserUpdateRequest
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
class UserControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `유저 생성 - 성공`() {
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
    fun `유저 생성 - 유효성 검사 실패`() {
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
    fun `전체 유저 조회 - 성공`() {
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
    fun `특정 유저 조회 - 성공`() {
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
    fun `특정 유저 조회 - 존재하지 않는 유저`() {
        mockMvc
            .perform(get("/users/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `유저 수정 - 성공`() {
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

        val updateRequest = UserUpdateRequest("홍길동수정", "updated@example.com")

        mockMvc
            .perform(
                put("/users/$userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value("홍길동수정"))
            .andExpect(jsonPath("$.email").value("updated@example.com"))
    }

    @Test
    fun `유저 수정 - 존재하지 않는 유저`() {
        val updateRequest = UserUpdateRequest("홍길동", "hong@example.com")

        mockMvc
            .perform(
                put("/users/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `유저 삭제 - 성공`() {
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
    fun `유저 삭제 - 존재하지 않는 유저`() {
        mockMvc
            .perform(delete("/users/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }
}
