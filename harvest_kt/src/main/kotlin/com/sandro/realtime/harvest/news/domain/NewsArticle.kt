package com.sandro.realtime.harvest.news.domain

import java.time.LocalDateTime

/**
 * 네이버 뉴스 기사 데이터 모델
 */
data class NewsArticle(
    val title: String,
    val content: String,
    val author: String?,
    val publishDate: LocalDateTime?,
    val mediaName: String,
    val articleId: String,
    val officeId: String,
    val imageUrl: String?,
    val description: String?,
    val sectionId: String?,
    val gdid: String?
)