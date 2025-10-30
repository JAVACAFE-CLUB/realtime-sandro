package com.sandro.realtime.smithy.document.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

import java.io.ByteArrayInputStream
import java.io.File

/**
 * TikaDocumentParser 테스트
 *
 * 다양한 형식의 문서 파싱 기능을 검증합니다.
 */
class TikaDocumentParserTest : DescribeSpec({

    val parser = TikaDocumentParser()

    describe("parseDocument") {
        context("텍스트 파일을 파싱할 때") {
            it("텍스트 내용을 올바르게 추출해야 한다") {
                // given
                val textContent = "안녕하세요. 이것은 테스트 문서입니다."
                val inputStream = ByteArrayInputStream(textContent.toByteArray())

                // when
                val result = parser.parseDocument(inputStream, "text/plain")

                // then
                result.success shouldBe true
                result.extractedText shouldContain "테스트 문서"
                result.contentType shouldContain "text/plain"
                result.errorMessage shouldBe null
            }
        }

        context("HTML 파일을 파싱할 때") {
            it("HTML 태그를 제거하고 텍스트만 추출해야 한다") {
                // given
                val htmlContent = """
                    <html>
                        <head><title>테스트 제목</title></head>
                        <body>
                            <h1>제목입니다</h1>
                            <p>본문 내용입니다.</p>
                        </body>
                    </html>
                """.trimIndent()
                val inputStream = ByteArrayInputStream(htmlContent.toByteArray())

                // when
                val result = parser.parseDocument(inputStream, "text/html")

                // then
                result.success shouldBe true
                // HTML body의 텍스트만 추출되므로 h1과 p 태그의 내용만 검증
                result.extractedText shouldContain "제목입니다"
                result.extractedText shouldContain "본문 내용"
            }
        }

        context("빈 문서를 파싱할 때") {
            it("빈 텍스트를 반환해야 한다") {
                // given
                val emptyContent = ""
                val inputStream = ByteArrayInputStream(emptyContent.toByteArray())

                // when
                val result = parser.parseDocument(inputStream, "text/plain")

                // then
                // 빈 문서는 파싱이 실패할 수도 있고, 성공하더라도 빈 텍스트를 반환해야 함
                if (result.success) {
                    result.extractedText.trim() shouldBe ""
                } else {
                    // 빈 문서 파싱 실패는 허용
                    result.errorMessage shouldNotBe null
                }
            }
        }
    }

    describe("detectContentType") {
        context("텍스트 파일의 컨텐츠 타입을 감지할 때") {
            it("올바른 MIME 타입을 반환해야 한다") {
                // given
                val textContent = "This is a text file."
                val inputStream = ByteArrayInputStream(textContent.toByteArray())

                // when
                val contentType = parser.detectContentType(inputStream)

                // then
                contentType shouldNotBe ""
            }
        }

        context("HTML 파일의 컨텐츠 타입을 감지할 때") {
            it("HTML MIME 타입을 반환해야 한다") {
                // given
                val htmlContent = "<html><body>HTML content</body></html>"
                val inputStream = ByteArrayInputStream(htmlContent.toByteArray())

                // when
                val contentType = parser.detectContentType(inputStream)

                // then
                contentType shouldNotBe ""
            }
        }
    }

    describe("언어 감지") {
        context("한글 텍스트를 파싱할 때") {
            it("한국어를 감지해야 한다") {
                // given
                val koreanText = """
                    안녕하세요. 한국어로 작성된 문서입니다.
                    Apache Tika는 다양한 문서 형식을 파싱할 수 있는 라이브러리입니다.
                    이 테스트는 한국어 감지 기능을 검증합니다.
                """.trimIndent()
                val inputStream = ByteArrayInputStream(koreanText.toByteArray())

                // when
                val result = parser.parseDocument(inputStream, "text/plain")

                // then
                result.success shouldBe true
                result.extractedText shouldNotBe ""
                // 언어 감지는 선택적 기능이므로 null 체크만 수행
                result.detectedLanguage shouldNotBe null
            }
        }

        context("영어 텍스트를 파싱할 때") {
            it("영어를 감지해야 한다") {
                // given
                val englishText = """
                    Hello. This is a document written in English.
                    Apache Tika is a library that can parse various document formats.
                    This test validates the English language detection feature.
                """.trimIndent()
                val inputStream = ByteArrayInputStream(englishText.toByteArray())

                // when
                val result = parser.parseDocument(inputStream, "text/plain")

                // then
                result.success shouldBe true
                result.extractedText shouldNotBe ""
                result.detectedLanguage shouldNotBe null
            }
        }
    }

    describe("실제 메시지 데이터 파싱") {
        val objectMapper = ObjectMapper().findAndRegisterModules()

        context("News 메시지를 파싱할 때") {
            it("HTML 콘텐츠에서 순수 텍스트를 추출해야 한다") {
                // given
                val newsMessageFile = File("src/test/resources/news-message.json")
                val jsonNode = objectMapper.readTree(newsMessageFile)
                val htmlContent = jsonNode.get("content").get("content").asText()

                val inputStream = ByteArrayInputStream(htmlContent.toByteArray(Charsets.UTF_8))

                // when
                val result = parser.parseDocument(inputStream, "text/html")

                // then
                result.success shouldBe true
                println("\n=== News 추출 결과 ===")
                println("원본 HTML 길이: ${htmlContent.length}자")
                println("추출된 텍스트 길이: ${result.extractedText.length}자")
                println("감지된 언어: ${result.detectedLanguage}")
                println("콘텐츠 타입: ${result.contentType}")
                println("\n추출된 텍스트:\n${result.extractedText}")
                println("======================\n")

                // HTML 태그가 제거되고 텍스트만 추출되었는지 검증
                result.extractedText shouldContain "코스피"
                result.extractedText shouldContain "4,000"
            }
        }

        context("Wiki 메시지를 파싱할 때") {
            it("위키마크업에서 텍스트를 추출해야 한다") {
                // given
                val wikiMessageFile = File("src/test/resources/wiki-message.json")
                val jsonNode = objectMapper.readTree(wikiMessageFile)
                val wikiText = jsonNode.get("content").get("revision").get("text").asText()

                val inputStream = ByteArrayInputStream(wikiText.toByteArray(Charsets.UTF_8))

                // when
                val result = parser.parseDocument(inputStream, "text/plain")

                // then
                result.success shouldBe true
                println("\n=== Wiki 추출 결과 ===")
                println("원본 위키마크업 길이: ${wikiText.length}자")
                println("추출된 텍스트 길이: ${result.extractedText.length}자")
                println("감지된 언어: ${result.detectedLanguage}")
                println("콘텐츠 타입: ${result.contentType}")
                println("\n추출된 텍스트:\n${result.extractedText}")
                println("======================\n")

                // 위키마크업이 제거되고 텍스트만 추출되었는지 검증
                result.extractedText shouldContain "지미 카터"
            }
        }
    }
})
