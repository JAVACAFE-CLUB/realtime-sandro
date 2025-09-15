package com.sandro.realtime.codex.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.dto.CreateDocumentRequest
import com.sandro.realtime.codex.dto.DocumentResponse
import com.sandro.realtime.codex.service.DocumentService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class DocumentControllerTest : DescribeSpec({

    val documentService = mockk<DocumentService>()
    val documentController = DocumentController(documentService)
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(documentController).build()
    val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    describe("POST /api/documents") {
        it("문서를 생성한다") {
            val request = CreateDocumentRequest(
                documentType = DocumentType.WIKI,
                title = "테스트 문서",
                content = "테스트 내용입니다.",
                source = "wikipedia",
                author = "tester",
                categories = listOf("테스트"),
                tags = listOf("sample"),
                url = "https://test.com"
            )
            val response = DocumentResponse(
                id = "test-id",
                documentType = DocumentType.WIKI,
                title = "테스트 문서",
                content = "테스트 내용",
                source = "wikipedia",
                publishedAt = null,
                author = "tester",
                categories = listOf("테스트"),
                tags = listOf("sample"),
                url = "https://test.com",
                metadata = emptyMap(),
                indexedAt = LocalDateTime.now()
            )

            every { documentService.createDocument(any()) } returns response

            mockMvc.perform(
                post("/api/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(response.id))
                .andExpect(jsonPath("$.documentType").value(response.documentType.name))

            verify(exactly = 1) { documentService.createDocument(any()) }
        }
    }

    describe("GET /api/documents/{id}") {
        context("문서가 존재할 때") {
            it("문서를 반환한다") {
                val documentId = "test-id"
                val response = DocumentResponse(
                    id = "test-id",
                    documentType = DocumentType.WIKI,
                    title = "테스트 문서",
                    content = "테스트 내용",
                    source = "wikipedia",
                    publishedAt = null,
                    author = "tester",
                    categories = listOf("테스트"),
                    tags = listOf("sample"),
                    url = "https://test.com",
                    metadata = emptyMap(),
                    indexedAt = LocalDateTime.now()
                )

                every { documentService.getDocument(documentId) } returns response

                mockMvc.perform(get("/api/documents/$documentId"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.id").value(response.id))
                    .andExpect(jsonPath("$.documentType").value(response.documentType.name))

                verify(exactly = 1) { documentService.getDocument(documentId) }
            }
        }

        context("문서가 존재하지 않을 때") {
            it("404를 반환한다") {
                val documentId = "non-existent-id"

                every { documentService.getDocument(documentId) } returns null

                mockMvc.perform(get("/api/documents/$documentId"))
                    .andExpect(status().isNotFound)

                verify(exactly = 1) { documentService.getDocument(documentId) }
            }
        }
    }

    describe("GET /api/documents/count") {
        it("전체 문서 개수를 반환한다") {
            every { documentService.count() } returns 100L

            mockMvc.perform(get("/api/documents/count"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.count").value(100))

            verify(exactly = 1) { documentService.count() }
        }
    }

    describe("DELETE /api/documents/{id}") {
        context("문서가 존재할 때") {
            it("문서를 삭제하고 204를 반환한다") {
                val documentId = "delete-id"

                every { documentService.deleteDocument(documentId) } returns true

                mockMvc.perform(delete("/api/documents/$documentId"))
                    .andExpect(status().isNoContent)

                verify(exactly = 1) { documentService.deleteDocument(documentId) }
            }
        }

        context("문서가 존재하지 않을 때") {
            it("404를 반환한다") {
                val documentId = "non-existent-id"

                every { documentService.deleteDocument(documentId) } returns false

                mockMvc.perform(delete("/api/documents/$documentId"))
                    .andExpect(status().isNotFound)

                verify(exactly = 1) { documentService.deleteDocument(documentId) }
            }
        }
    }
})