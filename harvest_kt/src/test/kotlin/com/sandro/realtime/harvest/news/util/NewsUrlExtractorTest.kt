package com.sandro.realtime.harvest.news.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class NewsUrlExtractorTest : DescribeSpec({

    describe("NewsUrlExtractor") {

        context("extractNewsUrls") {
            it("data-sample/naver_news.html에서 뉴스 URL을 추출해야 한다") {
                // given
                val htmlFilePath = "/Users/sandeulpark/personal/realtime/data-sample/naver_news.html"
                val file = File(htmlFilePath)

                // HTML 파일이 존재하는지 확인
                file.exists() shouldBe true

                // when
                val urls = NewsUrlExtractor.extractNewsUrls(htmlFilePath)

                // then
                urls.shouldNotBeEmpty()
                println("추출된 URL 개수: ${urls.size}")

                // 처음 5개 URL 출력
                urls.take(5).forEach { url ->
                    println("URL: $url")
                }
            }

            it("추출된 모든 URL은 올바른 네이버 뉴스 형식이어야 한다") {
                // given
                val htmlFilePath = "/Users/sandeulpark/personal/realtime/data-sample/naver_news.html"

                // when
                val urls = NewsUrlExtractor.extractNewsUrls(htmlFilePath)

                // then
                urls.forEach { url ->
                    url shouldMatch "^https://n\\.news\\.naver\\.com/article/[0-9]+/[0-9]+$".toRegex()
                }
            }

            it("URL 리스트는 중복이 제거되고 정렬되어야 한다") {
                // given
                val htmlFilePath = "/Users/sandeulpark/personal/realtime/data-sample/naver_news.html"

                // when
                val urls = NewsUrlExtractor.extractNewsUrls(htmlFilePath)

                // then
                val uniqueUrls = urls.distinct()
                urls.size shouldBe uniqueUrls.size // 중복 제거 확인

                val sortedUrls = urls.sorted()
                urls shouldBe sortedUrls // 정렬 확인
            }
        }

        context("isValidNewsUrl") {
            it("유효한 뉴스 URL을 검증해야 한다") {
                // given
                val validUrl = "https://n.news.naver.com/article/005/0001804194"

                // when
                val result = NewsUrlExtractor.isValidNewsUrl(validUrl)

                // then
                result shouldBe true
            }

            it("유효하지 않은 URL을 거부해야 한다") {
                // given
                val invalidUrls = listOf(
                    "https://news.naver.com/article/005/0001804194",
                    "https://n.news.naver.com/section/100",
                    "invalid-url",
                    ""
                )

                // when & then
                invalidUrls.forEach { url ->
                    NewsUrlExtractor.isValidNewsUrl(url) shouldBe false
                }
            }
        }

        context("extractMediaCode") {
            it("URL에서 언론사 코드를 추출해야 한다") {
                // given
                val url = "https://n.news.naver.com/article/005/0001804194"

                // when
                val mediaCode = NewsUrlExtractor.extractMediaCode(url)

                // then
                mediaCode shouldBe "005"
            }

            it("유효하지 않은 URL에서는 null을 반환해야 한다") {
                // given
                val invalidUrl = "https://invalid-url.com"

                // when
                val mediaCode = NewsUrlExtractor.extractMediaCode(invalidUrl)

                // then
                mediaCode shouldBe null
            }
        }

        context("extractArticleId") {
            it("URL에서 기사 번호를 추출해야 한다") {
                // given
                val url = "https://n.news.naver.com/article/005/0001804194"

                // when
                val articleId = NewsUrlExtractor.extractArticleId(url)

                // then
                articleId shouldBe "0001804194"
            }

            it("유효하지 않은 URL에서는 null을 반환해야 한다") {
                // given
                val invalidUrl = "https://invalid-url.com"

                // when
                val articleId = NewsUrlExtractor.extractArticleId(invalidUrl)

                // then
                articleId shouldBe null
            }
        }
    }
})