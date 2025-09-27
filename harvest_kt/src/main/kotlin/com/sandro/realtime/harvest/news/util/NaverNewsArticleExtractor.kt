package com.sandro.realtime.harvest.news.util

import com.sandro.realtime.harvest.news.domain.NewsArticle
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 네이버 뉴스 기사 HTML에서 데이터를 추출하는 구현체
 */
@Component
class NaverNewsArticleExtractor : NewsArticleExtractor {

    /**
     * HTML 문자열에서 네이버 뉴스 기사 데이터를 추출합니다.
     *
     * @param html HTML 문자열
     * @return NewsArticle 객체
     */
    override fun extractArticle(html: String): NewsArticle {
        val doc = Jsoup.parse(html)

        return NewsArticle(
            title = extractTitle(doc),
            url = extractUrl(doc),
            imageUrl = extractImageUrl(doc) ?: "",
            description = extractDescription(doc) ?: "",

            articleId = extractArticleId(html) ?: "",
            sectionId = extractSectionId(html),
            gdid = extractGdid(html) ?: "",

            officeId = extractOfficeId(html) ?: "",
            officeName = extractMediaName(doc) ?: "",
            officeCategory = extractOfficeCategory(html) ?: "",

            author = extractAuthor(doc) ?: "",
            createdAt = extractPublishDateString(doc) ?: "",
            lastModifiedAt = extractLastModifiedDateString(doc),
            originUrl = extractOriginUrl(doc) ?: "",

            content = extractContent(doc),
        )
    }

    /**
     * 제목 추출
     */
    private fun extractTitle(doc: Document): String {
        return doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?: doc.selectFirst(".media_end_head_headline")?.text()?.trim()
            ?: doc.title().trim()
    }

    /**
     * 대표 이미지 URL 추출
     */
    private fun extractImageUrl(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image]")?.attr("content")?.trim()
    }

    /**
     * 기사 요약 추출
     */
    private fun extractDescription(doc: Document): String? {
        return doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
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
        val dateTimeAttr = doc.selectFirst("._ARTICLE_DATE_TIME")?.attr("data-date-time")?.trim()

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
     * 발행일시 문자열 추출
     */
    private fun extractPublishDateString(doc: Document): String? {
        return doc.selectFirst("._ARTICLE_DATE_TIME")?.attr("data-date-time")?.trim()
    }

    /**
     * URL 추출
     */
    private fun extractUrl(doc: Document): String {
        return doc.selectFirst("meta[property=og:url]")?.attr("content")?.trim() ?: ""
    }

    /**
     * 언론사명 추출
     */
    private fun extractMediaName(doc: Document): String? {
        // JavaScript 변수에서 추출
        val scriptText = doc.select("script").text()
        val officeNamePattern = """office\s*=\s*\{[^}]*name\s*:\s*["']([^"']+)["']""".toRegex()
        val match = officeNamePattern.find(scriptText)

        return match?.groupValues?.get(1)?.trim()
            ?: doc.selectFirst("meta[property=og:article:author]")?.attr("content")?.split(" | ")?.firstOrNull()?.trim()
    }

    /**
     * 기사 ID 추출 (JavaScript 변수에서)
     */
    private fun extractArticleId(html: String): String? {
        val pattern = """articleId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)?.trim()
    }

    /**
     * 언론사 ID 추출 (JavaScript 변수에서)
     */
    private fun extractOfficeId(html: String): String? {
        val pattern = """officeId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)?.trim()
    }

    /**
     * 섹션 ID 추출 (JavaScript 변수에서)
     */
    private fun extractSectionId(html: String): String? {
        val pattern = """sectionId\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)?.trim()
    }

    /**
     * GDID 추출 (JavaScript 변수에서)
     */
    private fun extractGdid(html: String): String? {
        val pattern = """gdid\s*:\s*["']([^"']+)["']""".toRegex()
        return pattern.find(html)?.groupValues?.get(1)?.trim()
    }

    /**
     * 최종수정일시 추출
     */
    private fun extractLastModifiedDate(doc: Document): LocalDateTime? {
        // _ARTICLE_MODIFY_DATE_TIME 요소에서 data-date-time 속성 추출
        val dateTimeAttr = doc.selectFirst("._ARTICLE_MODIFY_DATE_TIME")?.attr("data-modify-date-time")?.trim()

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
     * 최종수정일시 문자열 추출
     */
    private fun extractLastModifiedDateString(doc: Document): String? {
        return doc.selectFirst("._ARTICLE_MODIFY_DATE_TIME")?.attr("data-modify-date-time")?.trim()
    }

    /**
     * 기사 원문 URL 추출
     */
    private fun extractOriginUrl(doc: Document): String? {
        return doc.selectFirst(".media_end_head_origin_link")?.attr("href")?.trim()
    }

    /**
     * 언론사 카테고리 추출 (JavaScript 변수에서)
     */
    private fun extractOfficeCategory(html: String): String? {
        val scriptText = html
        val officeCategoryPattern = """office\s*=\s*\{[^}]*category\s*:\s*["']([^"']+)["']""".toRegex()
        return officeCategoryPattern.find(scriptText)?.groupValues?.get(1)?.trim()
    }
}