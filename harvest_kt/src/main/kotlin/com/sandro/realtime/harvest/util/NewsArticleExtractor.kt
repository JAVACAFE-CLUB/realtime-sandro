package com.sandro.realtime.harvest.util

import com.sandro.realtime.harvest.domain.NewsArticle
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 네이버 뉴스 기사 HTML에서 데이터를 추출하는 유틸리티 클래스
 */
object NewsArticleExtractor {

    /**
     * HTML 문자열에서 뉴스 기사 데이터를 추출합니다.
     *
     * @param html HTML 문자열
     * @return NewsArticle 객체
     */
    fun extractArticle(html: String): NewsArticle {
        val doc = Jsoup.parse(html)

        return NewsArticle(
            title = extractTitle(doc),
            content = extractContent(doc),
            author = extractAuthor(doc),
            publishDate = extractPublishDate(doc),
            mediaName = extractMediaName(doc),
            articleId = extractArticleId(html),
            officeId = extractOfficeId(html),
            imageUrl = extractImageUrl(doc),
            description = extractDescription(doc),
            sectionId = extractSectionId(html),
            gdid = extractGdid(html)
        )
    }

    /**
     * 제목 추출
     */
    private fun extractTitle(doc: Document): String {
        return doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst(".media_end_head_headline")?.text()
            ?: doc.title()
    }

    /**
     * 본문 내용 추출
     */
    private fun extractContent(doc: Document): String {
        // 여러 가능한 본문 선택자들을 시도
        val contentSelectors = listOf(
            "#dic_area",
            ".go_trans.newsct_article",
            ".media_end_body_contents",
            "[data-module=ArticleViewer] .article_body",
            ".article_body"
        )

        for (selector in contentSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null && element.text().isNotEmpty()) {
                return element.text().trim()
            }
        }

        return ""
    }

    /**
     * 기자명 추출
     */
    private fun extractAuthor(doc: Document): String? {
        return doc.selectFirst(".media_end_head_journalist_name")?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:article:author]")?.attr("content")?.split(" | ")?.firstOrNull()
    }

    /**
     * 발행일시 추출
     */
    private fun extractPublishDate(doc: Document): LocalDateTime? {
        // data-date-time 속성에서 추출 (형식: "2025-09-23 19:07:44")
        val dateTimeAttr = doc.selectFirst("._ARTICLE_DATE_TIME")?.attr("data-date-time")
        
        return if (!dateTimeAttr.isNullOrEmpty()) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(dateTimeAttr, formatter)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 언론사명 추출
     */
    private fun extractMediaName(doc: Document): String {
        // JavaScript 변수에서 추출
        val scriptText = doc.select("script").text()
        val officeNamePattern = """office\s*=\s*\{[^}]*name\s*:\s*["']([^"']+)["']""".toRegex()
        val match = officeNamePattern.find(scriptText)

        return match?.groupValues?.get(1)
            ?: doc.selectFirst("meta[property=og:article:author]")?.attr("content")?.split(" | ")?.firstOrNull()
            ?: ""
    }

    /**
     * 기사 ID 추출 (JavaScript 변수에서)
     */
    private fun extractArticleId(html: String): String {
        val pattern = """articleId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1) ?: ""
    }

    /**
     * 언론사 ID 추출 (JavaScript 변수에서)
     */
    private fun extractOfficeId(html: String): String {
        val pattern = """officeId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1) ?: ""
    }

    /**
     * 대표 이미지 URL 추출
     */
    private fun extractImageUrl(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image]")?.attr("content")
    }

    /**
     * 기사 요약 추출
     */
    private fun extractDescription(doc: Document): String? {
        return doc.selectFirst("meta[property=og:description]")?.attr("content")
    }

    /**
     * 섹션 ID 추출 (JavaScript 변수에서)
     */
    private fun extractSectionId(html: String): String? {
        val pattern = """sectionId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)
    }

    /**
     * GDID 추출 (JavaScript 변수에서)
     */
    private fun extractGdid(html: String): String? {
        val pattern = """gdid\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)
    }
}