package com.sandro.realtime.harvest.service

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * 네이버 뉴스 HTML을 받아오는 서비스
 */
@Service
class NewsHtmlFetcher {
    
    companion object {
        private const val NAVER_NEWS_URL = "https://news.naver.com/"
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        private const val ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4"
        private const val ACCEPT_ENCODING = "gzip, deflate"
    }

    private val webClient = WebClient.builder()
        .codecs { configurer ->
            // 응답 처리 시 메모리 버퍼 (기본값 256KB)
            configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) // 5MB
        }
        .build()

    /**
     * 네이버 뉴스 메인 페이지 HTML을 받아옵니다.
     *
     * @param url 받아올 URL (기본값: 네이버 뉴스 메인)
     * @return HTML 문자열
     */
    suspend fun fetchHtml(url: String = NAVER_NEWS_URL): String {
        return try {
            webClient.get()
                .uri(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept-Encoding", ACCEPT_ENCODING)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(10))
                .awaitSingle()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch HTML from $url", e)
        }
    }

    /**
     * HTML 크기를 바이트 단위로 반환합니다.
     *
     * @param html HTML 문자열
     * @return 바이트 크기
     */
    fun getHtmlSize(html: String): Int {
        return html.toByteArray(Charsets.UTF_8).size
    }

    /**
     * HTML에 특정 패턴이 포함되어 있는지 확인합니다.
     *
     * @param html HTML 문자열
     * @param pattern 찾을 패턴
     * @return 포함 여부
     */
    fun containsPattern(html: String, pattern: String): Boolean {
        return html.contains(pattern)
    }
}