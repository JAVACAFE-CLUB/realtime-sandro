package com.sandro.realtime.harvest.news.service

import com.sandro.realtime.harvest.news.event.NewsArticleProcessedEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * 뉴스 수집을 담당하는 서비스 클래스
 * 병렬 처리를 통해 효율적인 뉴스 크롤링을 수행합니다.
 */
@Service
class NewsCollectionService(
    private val newsCrawlingService: NewsCrawlingService,
    private val articleService: ArticleService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(NewsCollectionService::class.java)

    fun collectNews() {
        runBlocking {
            try {
                val urls = newsCrawlingService.getNewsUrls()

                if (urls.isEmpty())
                    return@runBlocking

                // 10개씩 청크 단위로 병렬 처리
                urls.chunked(5).forEach { urlChunk ->
                    urlChunk.map { url ->
                        async {
                            processSingleNews(url)
                        }
                    }.awaitAll()

                    // 네이버 서버 부담 방지를 위한 대기
                    delay(1000)
                }

            } catch (e: Exception) {
                logger.error("뉴스 수집 중 전체 프로세스 오류 발생", e)
            }
        }
    }

    /**
     * 개별 뉴스를 처리합니다.
     */
    private suspend fun processSingleNews(url: String) {
        try {
            val article = newsCrawlingService.getArticleDetails(url)
            articleService.upsert(article)?.let {
                eventPublisher.publishEvent(NewsArticleProcessedEvent(it))
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }
}