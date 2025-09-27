package com.sandro.realtime.harvest.news.util

/**
 * 네이버 뉴스 HTML에서 뉴스 URL을 추출하는 유틸리티 클래스
 */
object NewsUrlExtractor {

    private val newsUrlPattern =
        """href="(https://n\.news\.naver\.com/article/\d+/\d+)"""".toRegex()

    /**
     * HTML 문자열에서 네이버 뉴스 URL을 추출합니다.
     *
     * @param html HTML 문자열
     * @return 추출된 뉴스 URL 리스트 (중복 제거, 정렬됨)
     */
    fun extractNewsUrls(html: String): List<String> {
        return try {
            newsUrlPattern.findAll(html)
                .map { it.groupValues[1] }
                .distinct()
                .sorted()
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}