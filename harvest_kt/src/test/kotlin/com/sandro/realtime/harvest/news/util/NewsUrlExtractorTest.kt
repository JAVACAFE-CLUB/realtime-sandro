package com.sandro.realtime.harvest.news.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class NewsUrlExtractorTest : DescribeSpec({

    describe("NewsUrlExtractor") {

        context("extractNewsUrls") {
            it("HTML 문자열에서 뉴스 URL을 추출해야 한다") {
                // given
                val sampleHtml = """
                    <html>
                    <body>
                        <a href="https://n.news.naver.com/article/001/0000001234">뉴스1</a>
                        <a href="https://n.news.naver.com/article/002/0000005678">뉴스2</a>
                        <a href="https://n.news.naver.com/article/001/0000001234">중복 뉴스1</a>
                        <a href="https://news.naver.com/invalid">잘못된 링크</a>
                    </body>
                    </html>
                """.trimIndent()

                // when
                val urls = NewsUrlExtractor.extractNewsUrls(sampleHtml)

                // then
                urls.shouldNotBeEmpty()
                urls.size shouldBe 2 // 중복 제거됨
                urls shouldBe listOf(
                    "https://n.news.naver.com/article/001/0000001234",
                    "https://n.news.naver.com/article/002/0000005678"
                ) // 정렬됨
            }

            it("data-sample/naver_news.html 파일에서도 URL을 추출해야 한다") {
                // given
                val htmlFilePath = "/Users/sandeulpark/personal/realtime/data-sample/naver_news.html"
                val file = File(htmlFilePath)

                if (file.exists()) {
                    val html = Files.readString(Paths.get(htmlFilePath))

                    // when
                    val urls = NewsUrlExtractor.extractNewsUrls(html)

                    // then
                    urls.shouldNotBeEmpty()
                    println("추출된 URL 개수: ${urls.size}")

                    // 처음 5개 URL 출력
                    urls.take(5).forEach { url ->
                        println("URL: $url")
                    }

                    // 추출된 모든 URL은 올바른 형식이어야 함
                    urls.forEach { url ->
                        url shouldMatch "^https://n\\.news\\.naver\\.com/article/[0-9]+/[0-9]+$".toRegex()
                    }
                }
            }

            it("빈 문자열에서는 빈 리스트를 반환해야 한다") {
                // given
                val emptyHtml = ""

                // when
                val urls = NewsUrlExtractor.extractNewsUrls(emptyHtml)

                // then
                urls.size shouldBe 0
            }
        }
    }
})