package com.sandro.realtime.harvest.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsHtmlFetcherTest : DescribeSpec({

    val newsHtmlFetcher = NewsHtmlFetcher()
    lateinit var sharedHtml: String

    beforeSpec {
        // 실제 HTML 한 번만 받아오기
        println("🔄 테스트용 HTML 받아오는 중...")
        sharedHtml = runBlocking { newsHtmlFetcher.fetchHtml() }
        println("✅ HTML 받아오기 완료")
    }

    describe("NewsHtmlFetcher") {

        context("fetchHtml - 실제 네트워크 호출") {
            it("네이버 뉴스 메인 페이지 HTML을 성공적으로 받아와야 한다") {
                // given - beforeSpec에서 이미 받아옴

                // then
                sharedHtml shouldContain "네이버 뉴스"
                sharedHtml shouldContain "<!doctype html>"
                sharedHtml shouldContain "news.naver.com"

                println("📊 HTML 크기: ${newsHtmlFetcher.getHtmlSize(sharedHtml)} bytes")
                println("📋 HTML 샘플 (처음 200자):")
                println(sharedHtml.take(200))
            }

            it("HTML 크기는 0보다 커야 한다") {
                // when
                val size = newsHtmlFetcher.getHtmlSize(sharedHtml)

                // then
                size shouldBeGreaterThan 0
                // 일반적으로 네이버 뉴스 페이지는 300KB 이상
                size shouldBeGreaterThan 300_000
                println("✅ HTML 크기 검증 통과: ${size} bytes")
            }

            it("HTML에 뉴스 기사 링크 패턴이 포함되어 있어야 한다") {
                // when
                val containsNewsLinks = newsHtmlFetcher.containsPattern(sharedHtml, "n.news.naver.com/article")

                // then
                containsNewsLinks shouldBe true
                println("✅ 뉴스 링크 패턴 포함 여부: $containsNewsLinks")
            }
        }

        context("getHtmlSize") {
            it("HTML 크기를 정확히 계산해야 한다") {
                // given
                val testHtml = "안녕하세요 테스트입니다"

                // when
                val size = newsHtmlFetcher.getHtmlSize(testHtml)

                // then
                // UTF-8에서 한글은 3바이트씩 차지
                size shouldBe testHtml.toByteArray(Charsets.UTF_8).size
            }
        }

        context("containsPattern") {
            it("패턴이 포함된 경우 true를 반환해야 한다") {
                // given
                val html = "<html><body>n.news.naver.com/article/123/456</body></html>"

                // when
                val result = newsHtmlFetcher.containsPattern(html, "n.news.naver.com/article")

                // then
                result shouldBe true
            }

            it("패턴이 포함되지 않은 경우 false를 반환해야 한다") {
                // given
                val html = "<html><body>일반적인 HTML 내용</body></html>"

                // when
                val result = newsHtmlFetcher.containsPattern(html, "n.news.naver.com/article")

                // then
                result shouldBe false
            }
        }
    }
})