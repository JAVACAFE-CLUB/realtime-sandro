package com.sandro.realtime.codex.repository

import com.sandro.realtime.codex.domain.Article
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : ElasticsearchRepository<Article, String> {
    fun findByTitleContaining(title: String): List<Article>
    fun findByContentContaining(content: String): List<Article>
}