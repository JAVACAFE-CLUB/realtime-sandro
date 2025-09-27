package com.sandro.realtime.harvest.news.domain

/**
 * 네이버 뉴스 기사 데이터 모델
 */
data class NewsArticle(
    val title: String,
    val url: String,
    val imageUrl: String,
    val description: String,

    val articleId: String,
    val sectionId: String?,
    val gdid: String,

    val officeId: String,
    val officeName: String,
    val officeCategory: String,

    val author: String,         // 기자
    val createdAt: String,      // 생성일시
    val lastModifiedAt: String?, // 최종수정일시
    val originUrl: String,      // 기사원문url

    val content: String,  // 본문
) {
    fun hasLastModifiedAt(): Boolean {
        return lastModifiedAt != null
    }

    fun hasUpdated(lastModifiedAt: String): Boolean {
        return (this.lastModifiedAt?.compareTo(lastModifiedAt) ?: 0) == 1
    }
}