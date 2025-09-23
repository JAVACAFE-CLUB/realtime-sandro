package com.sandro.realtime.harvest.service

import com.sandro.realtime.harvest.domain.NewsArticle
import com.sandro.realtime.harvest.util.NewsArticleExtractor
import com.sandro.realtime.harvest.util.NewsUrlExtractor
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * 뉴스 HTML 받아오기와 URL 추출을 통합한 서비스
 */
@Service
class NewsService(
    private val newsHtmlFetcher: NewsHtmlFetcher
) {

    /**
     * 실시간으로 네이버 뉴스 URL을 추출합니다.
     *
     * @return 추출된 뉴스 URL 리스트
     */
    suspend fun getNewsUrls(): List<String> {
        val html = newsHtmlFetcher.fetchHtml()
        return extractUrlsFromHtml(html)
    }

    /**
     * HTML에서 뉴스 URL을 추출합니다.
     *
     * @param html HTML 문자열
     * @return 추출된 뉴스 URL 리스트
     */
    fun extractUrlsFromHtml(html: String): List<String> {
        return try {
            // 임시 파일 생성
            val tempFile = Files.createTempFile("naver_news", ".html")

            // HTML을 임시 파일에 저장
            Files.write(tempFile, html.toByteArray(), StandardOpenOption.WRITE)

            // NewsUrlExtractor로 URL 추출
            val urls = NewsUrlExtractor.extractNewsUrls(tempFile.toAbsolutePath().toString())

            // 임시 파일 삭제
            Files.deleteIfExists(tempFile)

            urls
        } catch (e: Exception) {
            throw RuntimeException("Failed to extract URLs from HTML", e)
        }
    }

    /**
     * HTML 크기와 URL 개수 통계를 반환합니다.
     *
     * @return 통계 정보
     */
    suspend fun getNewsStatistics(): NewsStatistics {
        val html = newsHtmlFetcher.fetchHtml()
        val urls = extractUrlsFromHtml(html)

        return NewsStatistics(
            htmlSize = newsHtmlFetcher.getHtmlSize(html),
            urlCount = urls.size,
            sampleUrls = urls.take(3)
        )
    }
    
    /**
     * 특정 기사 URL에서 기사 상세 정보를 추출합니다.
     * 
     * @param articleUrl 기사 URL
     * @return NewsArticle 객체
     */
    suspend fun getArticleDetails(articleUrl: String): NewsArticle {
        val html = newsHtmlFetcher.fetchHtml(articleUrl)
        return NewsArticleExtractor.extractArticle(html)
    }
    
    /**
     * HTML 문자열에서 직접 기사 정보를 추출합니다.
     * 
     * @param html HTML 문자열
     * @return NewsArticle 객체
     */
    fun extractArticleFromHtml(html: String): NewsArticle {
        return NewsArticleExtractor.extractArticle(html)
    }
}

/**
 * 뉴스 통계 데이터 클래스
 */
data class NewsStatistics(
    val htmlSize: Int,
    val urlCount: Int,
    val sampleUrls: List<String>
)