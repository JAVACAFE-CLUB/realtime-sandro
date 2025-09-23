package com.sandro.realtime.harvest.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsServiceTest : DescribeSpec({

    lateinit var sharedHtml: String
    val mockHtmlFetcher = mockk<NewsHtmlFetcher>()
    lateinit var newsService: NewsService

    beforeSpec {
        // 실제 HTML 한 번만 받아오기
        println("🔄 실제 네이버 뉴스 HTML 받아오는 중...")
        val realFetcher = NewsHtmlFetcher()
        sharedHtml = runBlocking { realFetcher.fetchHtml() }
        println("✅ HTML 받아오기 완료 (크기: ${sharedHtml.length} 문자)")

        // Mock 설정 - 모든 테스트에서 같은 HTML 사용
        coEvery { mockHtmlFetcher.fetchHtml() } returns sharedHtml
        coEvery { mockHtmlFetcher.getHtmlSize(any()) } answers {
            firstArg<String>().toByteArray(Charsets.UTF_8).size
        }
        coEvery { mockHtmlFetcher.containsPattern(any(), any()) } answers {
            firstArg<String>().contains(secondArg<String>())
        }

        // NewsService에 Mock 주입
        newsService = NewsService(mockHtmlFetcher)
    }

    describe("NewsService") {

        context("getNewsUrls") {
            it("실시간 뉴스 URL을 성공적으로 추출해야 한다") {
                runBlocking {
                    // when
                    val urls = newsService.getNewsUrls()

                    // then
                    urls.shouldNotBeEmpty()
                    println("📊 추출된 URL 개수: ${urls.size}")

                    // 처음 5개 URL 출력
                    urls.take(5).forEachIndexed { index, url ->
                        println("${index + 1}. $url")
                    }

                    // 모든 URL이 올바른 형식인지 확인
                    urls.forEach { url ->
                        url shouldMatch "^https://n\\.news\\.naver\\.com/article/[0-9]+/[0-9]+$".toRegex()
                    }
                }
            }

            it("추출된 URL 개수가 합리적 범위에 있어야 한다") {
                runBlocking {
                    // when
                    val urls = newsService.getNewsUrls()

                    // then
                    // 네이버 뉴스 메인 페이지에는 최소 100개 이상의 뉴스가 있어야 함
                    urls.size shouldBeGreaterThan 100
                    println("✅ URL 개수 검증 통과: ${urls.size}개")
                }
            }
        }

        context("extractUrlsFromHtml") {
            it("HTML 문자열에서 직접 URL을 추출할 수 있어야 한다") {
                // given
                val testHtml = """
                    <html>
                    <body>
                        <a href="https://n.news.naver.com/article/001/0001234567">뉴스1</a>
                        <a href="https://n.news.naver.com/article/020/0009876543">뉴스2</a>
                        <a href="https://other.site.com">다른사이트</a>
                    </body>
                    </html>
                """.trimIndent()

                // when
                val urls = newsService.extractUrlsFromHtml(testHtml)

                // then
                urls.size shouldBe 2
                urls shouldBe listOf(
                    "https://n.news.naver.com/article/001/0001234567",
                    "https://n.news.naver.com/article/020/0009876543"
                )
            }

            it("빈 HTML에서는 빈 리스트를 반환해야 한다") {
                // given
                val emptyHtml = "<html><body></body></html>"

                // when
                val urls = newsService.extractUrlsFromHtml(emptyHtml)

                // then
                urls shouldBe emptyList()
            }
        }

        context("getNewsStatistics") {
            it("뉴스 통계 정보를 정확히 반환해야 한다") {
                runBlocking {
                    // when
                    val stats = newsService.getNewsStatistics()

                    // then
                    stats.htmlSize shouldBeGreaterThan 0
                    stats.urlCount shouldBeGreaterThan 0
                    stats.sampleUrls.size shouldBe minOf(3, stats.urlCount)

                    println("📈 뉴스 통계:")
                    println("  - HTML 크기: ${stats.htmlSize} bytes")
                    println("  - URL 개수: ${stats.urlCount}")
                    println("  - 샘플 URL들:")
                    stats.sampleUrls.forEach { url ->
                        println("    * $url")
                    }
                }
            }
        }
    }
})