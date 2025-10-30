package com.sandro.realtime.harvest.news.service

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import com.navercorp.fixturemonkey.kotlin.giveMeOne
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.repository.SourceContentRepository
import com.sandro.realtime.harvest.news.domain.NewsArticle
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher

class NewsCollectionServiceTest : DescribeSpec({

    val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .build()
    val newsCrawlingService = mockk<NewsCrawlingService>()
    val sourceContentRepository = mockk<SourceContentRepository>()
    val articleService = ArticleService(sourceContentRepository)
    val eventPublisher = mockk<ApplicationEventPublisher>()

    val newsCollectionService = NewsCollectionService(
        newsCrawlingService = newsCrawlingService,
        articleService = articleService,
        eventPublisher = eventPublisher
    )

    beforeEach {
        clearAllMocks()
    }

    describe("collectNews") {
        context("URL 목록이 비어있을 때") {
            it("처리를 스킵한다") {
                runBlocking {
                    // given
                    coEvery { newsCrawlingService.getNewsUrls() } returns emptyList()

                    // when
                    newsCollectionService.collectNews()

                    // then
                    coVerify(exactly = 1) { newsCrawlingService.getNewsUrls() }
                    coVerify(exactly = 0) { newsCrawlingService.getArticleDetails(any()) }
                }
            }
        }

        context("URL 목록이 있을 때") {
            it("병렬로 뉴스를 처리한다") {
                runBlocking {
                    // given
                    val urls = listOf(
                        "https://n.news.naver.com/article/001/0000001234",
                        "https://n.news.naver.com/article/002/0000005678"
                    )
                    val newsArticle = fixtureMonkey.giveMeOne<NewsArticle>()
                    val sourceContent = fixtureMonkey.giveMeKotlinBuilder<SourceContent>()
                        .setExp(SourceContent::id, "test-content-id")
                        .setExp(SourceContent::type, SourceType.NEWS)
                        .sample()

                    coEvery { newsCrawlingService.getNewsUrls() } returns urls
                    coEvery { newsCrawlingService.getArticleDetails(any()) } returns newsArticle
                    every {
                        sourceContentRepository.findByTypeAndContentArticleId(
                            any(),
                            any()
                        )
                    } returns java.util.Optional.empty()
                    every { sourceContentRepository.save(any()) } returns sourceContent
                    every { eventPublisher.publishEvent(any()) } just Runs

                    // when
                    newsCollectionService.collectNews()

                    // then
                    coVerify(exactly = 1) { newsCrawlingService.getNewsUrls() }
                    coVerify(exactly = urls.size) { newsCrawlingService.getArticleDetails(any()) }
                    verify(exactly = urls.size) { sourceContentRepository.save(any()) }
                    verify(exactly = urls.size) { eventPublisher.publishEvent(any()) }
                }
            }
        }

        context("뉴스 처리 중 오류 발생 시") {
            it("에러 카운트를 증가시키지만 전체 프로세스는 계속 진행한다") {
                runBlocking {
                    // given
                    val urls = listOf(
                        "https://n.news.naver.com/article/001/0000001234",
                        "https://n.news.naver.com/article/002/0000005678"
                    )
                    val newsArticle = fixtureMonkey.giveMeOne<NewsArticle>()
                    val sourceContent = fixtureMonkey.giveMeKotlinBuilder<SourceContent>()
                        .setExp(SourceContent::id, "test-content-id")
                        .setExp(SourceContent::type, SourceType.NEWS)
                        .sample()

                    coEvery { newsCrawlingService.getNewsUrls() } returns urls
                    coEvery { newsCrawlingService.getArticleDetails(urls[0]) } throws RuntimeException("첫 번째 뉴스 실패")
                    coEvery { newsCrawlingService.getArticleDetails(urls[1]) } returns newsArticle
                    every {
                        sourceContentRepository.findByTypeAndContentArticleId(
                            any(),
                            any()
                        )
                    } returns java.util.Optional.empty()
                    every { sourceContentRepository.save(any()) } returns sourceContent
                    every { eventPublisher.publishEvent(any()) } just Runs

                    // when
                    newsCollectionService.collectNews()

                    // then
                    coVerify(exactly = 1) { newsCrawlingService.getNewsUrls() }
                    coVerify(exactly = urls.size) { newsCrawlingService.getArticleDetails(any()) }
                    verify(exactly = 1) { sourceContentRepository.save(any()) } // 성공한 것만
                    verify(exactly = 1) { eventPublisher.publishEvent(any()) } // 성공한 것만
                }
            }
        }
    }
})