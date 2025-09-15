package com.sandro.realtime.codex.service

import com.sandro.realtime.codex.domain.Article
import com.sandro.realtime.codex.repository.ArticleRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*
import kotlin.test.*

@ExtendWith(MockitoExtension::class)
class ArticleServiceTest {

    @Mock
    private lateinit var articleRepository: ArticleRepository

    @InjectMocks
    private lateinit var articleService: ArticleService

    @Test
    fun `새로운 Article을 생성할 때 저장되고 반환된다`() {
        val title = "테스트 제목"
        val content = "테스트 내용"
        val savedArticle = Article(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(articleRepository.save(any<Article>())).thenReturn(savedArticle)

        val result = articleService.create(title, content)

        assertEquals(savedArticle, result)
        verify(articleRepository).save(any<Article>())
    }

    @Test
    fun `ID로 Article 조회시 해당 Article이 반환된다`() {
        val id = UUID.randomUUID().toString()
        val article = Article(
            id = id,
            title = "제목",
            content = "내용",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(articleRepository.findById(id)).thenReturn(Optional.of(article))

        val result = articleService.findById(id)

        assertNotNull(result)
        assertEquals(article, result)
        verify(articleRepository).findById(id)
    }

    @Test
    fun `존재하지 않는 ID로 Article 조회시 null이 반환된다`() {
        val id = UUID.randomUUID().toString()

        whenever(articleRepository.findById(id)).thenReturn(Optional.empty())

        val result = articleService.findById(id)

        assertNull(result)
        verify(articleRepository).findById(id)
    }

    @Test
    fun `모든 Article 조회시 목록이 반환된다`() {
        val articles = listOf(
            Article(
                id = UUID.randomUUID().toString(),
                title = "제목1",
                content = "내용1",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        whenever(articleRepository.findAll()).thenReturn(articles)

        val result = articleService.findAll()

        assertEquals(articles, result)
        verify(articleRepository).findAll()
    }

    @Test
    fun `Article 수정시 수정된 Article이 반환된다`() {
        val id = UUID.randomUUID().toString()
        val existingArticle = Article(
            id = id,
            title = "기존 제목",
            content = "기존 내용",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now().minusDays(1)
        )
        val updatedTitle = "수정된 제목"
        val updatedContent = "수정된 내용"

        whenever(articleRepository.findById(id)).thenReturn(Optional.of(existingArticle))
        whenever(articleRepository.save(any<Article>())).thenReturn(
            existingArticle.copy(
                title = updatedTitle,
                content = updatedContent,
                updatedAt = LocalDateTime.now()
            )
        )

        val result = articleService.update(id, updatedTitle, updatedContent)

        assertNotNull(result)
        assertEquals(updatedTitle, result.title)
        assertEquals(updatedContent, result.content)
        verify(articleRepository).findById(id)
        verify(articleRepository).save(any<Article>())
    }

    @Test
    fun `Article 삭제시 성공한다`() {
        val id = UUID.randomUUID().toString()

        whenever(articleRepository.existsById(id)).thenReturn(true)

        val result = articleService.delete(id)

        assertTrue(result)
        verify(articleRepository).existsById(id)
        verify(articleRepository).deleteById(id)
    }

    @Test
    fun `존재하지 않는 Article 삭제시 실패한다`() {
        val id = UUID.randomUUID().toString()

        whenever(articleRepository.existsById(id)).thenReturn(false)

        val result = articleService.delete(id)

        assertFalse(result)
        verify(articleRepository).existsById(id)
    }

    @Test
    fun `제목으로 Article 검색시 결과가 반환된다`() {
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

        whenever(articleRepository.findByTitleContaining(keyword)).thenReturn(articles)

        val result = articleService.searchByTitle(keyword)

        assertEquals(articles, result)
        verify(articleRepository).findByTitleContaining(keyword)
    }

    @Test
    fun `내용으로 Article 검색시 결과가 반환된다`() {
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

        whenever(articleRepository.findByContentContaining(keyword)).thenReturn(articles)

        val result = articleService.searchByContent(keyword)

        assertEquals(articles, result)
        verify(articleRepository).findByContentContaining(keyword)
    }
}