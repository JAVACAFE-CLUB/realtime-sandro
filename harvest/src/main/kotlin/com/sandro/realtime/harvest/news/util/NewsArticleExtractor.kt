package com.sandro.realtime.harvest.news.util

import com.sandro.realtime.harvest.news.domain.NewsArticle

/**
 * 뉴스 기사 HTML에서 데이터를 추출하는 인터페이스
 * 각 뉴스 사이트별로 다른 구현체를 만들어 사용할 수 있습니다.
 */
interface NewsArticleExtractor {

    /**
     * HTML 문자열에서 뉴스 기사 데이터를 추출합니다.
     *
     * @param html HTML 문자열
     * @return NewsArticle 객체
     */
    fun extractArticle(html: String): NewsArticle
}