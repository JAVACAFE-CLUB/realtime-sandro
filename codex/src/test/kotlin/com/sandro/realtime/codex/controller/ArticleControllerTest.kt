package com.sandro.realtime.codex.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.sandro.realtime.codex.domain.Article
import com.sandro.realtime.codex.dto.CreateArticleRequest
import com.sandro.realtime.codex.dto.UpdateArticleRequest
import com.sandro.realtime.codex.service.ArticleService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(ArticleController::class)
class ArticleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var articleService: ArticleService

    @Test
    fun `POST api_articles - Article 생성 성공`() {
        val request = CreateArticleRequest(
            title = "테스트 제목",
            content = "테스트 내용"
        )
        val article = Article(
            id = UUID.randomUUID().toString(),
            title = request.title,
            content = request.content,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(articleService.create(any(), any())).thenReturn(article)

        mockMvc.perform(
            post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(article.id))
            .andExpect(jsonPath("$.title").value(article.title))
            .andExpect(jsonPath("$.content").value(article.content))
    }

    @Test
    fun `GET api_articles_id - Article 조회 성공`() {
        val id = UUID.randomUUID().toString()
        val article = Article(
            id = id,
            title = "테스트 제목",
            content = "테스트 내용",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(articleService.findById(id)).thenReturn(article)

        mockMvc.perform(get("/api/articles/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(article.id))
            .andExpect(jsonPath("$.title").value(article.title))
            .andExpect(jsonPath("$.content").value(article.content))
    }

    @Test
    fun `GET api_articles_id - Article이 존재하지 않을 때 404 반환`() {
        val id = UUID.randomUUID().toString()

        whenever(articleService.findById(id)).thenReturn(null)

        mockMvc.perform(get("/api/articles/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET api_articles - 모든 Article 조회 성공`() {
        val articles = listOf(
            Article(
                id = UUID.randomUUID().toString(),
                title = "제목1",
                content = "내용1",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        whenever(articleService.findAll()).thenReturn(articles)

        mockMvc.perform(get("/api/articles"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `PUT api_articles_id - Article 수정 성공`() {
        val id = UUID.randomUUID().toString()
        val request = UpdateArticleRequest(
            title = "수정된 제목",
            content = "수정된 내용"
        )
        val updatedArticle = Article(
            id = id,
            title = request.title ?: "기본 제목",
            content = request.content ?: "기본 내용",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        whenever(articleService.update(any(), any(), any())).thenReturn(updatedArticle)

        mockMvc.perform(
            put("/api/articles/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(updatedArticle.id))
    }

    @Test
    fun `DELETE api_articles_id - Article 삭제 성공`() {
        val id = UUID.randomUUID().toString()

        whenever(articleService.delete(id)).thenReturn(true)

        mockMvc.perform(delete("/api/articles/$id"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE api_articles_id - Article이 존재하지 않을 때 404 반환`() {
        val id = UUID.randomUUID().toString()

        whenever(articleService.delete(id)).thenReturn(false)

        mockMvc.perform(delete("/api/articles/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET api_articles_search_title - 제목으로 검색 성공`() {
        val keyword = "검색어"
        val articles = listOf(
            Article(
                id = UUID.randomUUID().toString(),
                title = "검색어 포함 제목",
                content = "내용",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        whenever(articleService.searchByTitle(keyword)).thenReturn(articles)

        mockMvc.perform(
            get("/api/articles/search/title")
                .param("keyword", keyword)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET api_articles_search_content - 내용으로 검색 성공`() {
        val keyword = "검색어"
        val articles = listOf(
            Article(
                id = UUID.randomUUID().toString(),
                title = "제목",
                content = "검색어 포함 내용",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        whenever(articleService.searchByContent(keyword)).thenReturn(articles)

        mockMvc.perform(
            get("/api/articles/search/content")
                .param("keyword", keyword)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
}