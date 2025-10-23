package com.sandro.realtime.harvest.news.service

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@Ignored
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

                println("📊 HTML 크기: ${sharedHtml.length} chars")
                println("📋 HTML 샘플 (처음 200자):")
                println(sharedHtml.take(200))
            }

            it("HTML 길이는 0보다 커야 한다") {
                // when
                val length = sharedHtml.length

                // then
                length shouldBeGreaterThan 0
                // 일반적으로 네이버 뉴스 페이지는 상당히 길다
                length shouldBeGreaterThan 100_000
                println("✅ HTML 길이 검증 통과: ${length} chars")
            }

            it("HTML에 뉴스 기사 링크 패턴이 포함되어 있어야 한다") {
                // when
                val containsNewsLinks = sharedHtml.contains("n.news.naver.com/article")

                // then
                containsNewsLinks shouldBe true
                println("✅ 뉴스 링크 패턴 포함 여부: $containsNewsLinks")
            }
        }

    }
})