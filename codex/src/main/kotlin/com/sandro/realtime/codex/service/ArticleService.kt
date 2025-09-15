package com.sandro.realtime.codex.service

import com.sandro.realtime.codex.domain.Article
import com.sandro.realtime.codex.repository.ArticleRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class ArticleService(
    private val articleRepository: ArticleRepository
) {
    fun create(title: String, content: String): Article {
        val article = Article(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return articleRepository.save(article)
    }

    fun findById(id: String): Article? {
        return articleRepository.findById(id).orElse(null)
    }

    fun findAll(): List<Article> {
        return articleRepository.findAll().toList()
    }

    fun update(id: String, title: String?, content: String?): Article? {
        val article = findById(id) ?: return null

        val updatedArticle = article.copy(
            title = title ?: article.title,
            content = content ?: article.content,
            updatedAt = LocalDateTime.now()
        )

        return articleRepository.save(updatedArticle)
    }

    fun delete(id: String): Boolean {
        return if (articleRepository.existsById(id)) {
            articleRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    fun searchByTitle(title: String): List<Article> {
        return articleRepository.findByTitleContaining(title)
    }

    fun searchByContent(content: String): List<Article> {
        return articleRepository.findByContentContaining(content)
    }
}