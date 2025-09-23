package com.sandro.realtime.harvest.domain

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

/**
 * 기사 메타 정보만 포함한 간단한 모델
 */
data class NewsArticleMeta(
    val title: String,
    val author: String?,
    val mediaName: String,
    val articleId: String,
    val officeId: String,
    val imageUrl: String?,
    val description: String?
)