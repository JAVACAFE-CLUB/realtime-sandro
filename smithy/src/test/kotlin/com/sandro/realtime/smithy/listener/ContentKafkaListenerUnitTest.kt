package com.sandro.realtime.smithy.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.smithy.document.service.DocumentParseResult
import com.sandro.realtime.smithy.document.service.TikaDocumentParser
import com.sandro.realtime.smithy.kafka.TextKafkaService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.support.Acknowledgment
import java.io.InputStream
import java.time.LocalDateTime

/**
 * ContentKafkaListener 단위 테스트
 *
 * Kafka 메시지 수신 및 Tika를 사용한 텍스트 추출 기능을 검증합니다.
 */
class ContentKafkaListenerUnitTest : DescribeSpec({

    lateinit var tikaDocumentParser: TikaDocumentParser
    lateinit var textKafkaService: TextKafkaService
    lateinit var objectMapper: ObjectMapper
    lateinit var contentKafkaListener: ContentKafkaListener
    lateinit var acknowledgment: Acknowledgment

    beforeEach {
        tikaDocumentParser = mockk()
        textKafkaService = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        contentKafkaListener = ContentKafkaListener(tikaDocumentParser, textKafkaService, objectMapper)
        acknowledgment = mockk(relaxed = true)
    }

    describe("handleWikiContentProcessed") {
        context("Wikipedia 메시지를 수신했을 때") {
            it("revision.text를 추출하여 Tika로 파싱하고 로그를 출력해야 한다") {
                // given
                val wikiText = """
                    == 개요 ==
                    테스트 위키 페이지입니다.

                    == 상세 ==
                    상세 내용이 여기에 들어갑니다.
                """.trimIndent()

                val message = ContentProcessedMessage(
                    id = "wiki-123",
                    type = SourceType.WIKIPEDIA,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "id" to 12345L,
                        "title" to "테스트 페이지",
                        "revision" to mapOf(
                            "id" to 67890L,
                            "text" to wikiText
                        )
                    )
                )

                val parseResult = DocumentParseResult(
                    extractedText = "개요\n테스트 위키 페이지입니다.\n\n상세\n상세 내용이 여기에 들어갑니다.",
                    contentType = "text/plain",
                    metadata = emptyMap(),
                    detectedLanguage = "ko",
                    success = true,
                    errorMessage = null
                )

                every {
                    tikaDocumentParser.parseDocument(any<InputStream>(), "text/plain")
                } returns parseResult

                justRun {
                    textKafkaService.sendTextExtracted(any())
                }

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleWikiContentProcessed(messageJson, acknowledgment)

                // then
                verify(exactly = 1) {
                    tikaDocumentParser.parseDocument(any<InputStream>(), "text/plain")
                }
                verify(exactly = 1) {
                    textKafkaService.sendTextExtracted(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("revision.text가 없는 메시지를 수신했을 때") {
            it("에러를 처리하고 커밋하지 않아야 한다") {
                // given
                val message = ContentProcessedMessage(
                    id = "wiki-456",
                    type = SourceType.WIKIPEDIA,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "id" to 12345L,
                        "title" to "테스트 페이지",
                        "revision" to mapOf(
                            "id" to 67890L
                            // text 필드 없음
                        )
                    )
                )

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleWikiContentProcessed(messageJson, acknowledgment)

                // then
                verify(exactly = 0) {
                    tikaDocumentParser.parseDocument(any<InputStream>(), any())
                }
                verify(exactly = 0) {
                    acknowledgment.acknowledge()
                }
            }
        }
    }

    describe("handleNewsContentProcessed") {
        context("News 메시지를 수신했을 때") {
            it("content를 추출하여 Tika로 파싱하고 로그를 출력해야 한다") {
                // given
                val htmlContent = """
                    <html>
                        <body>
                            <h1>뉴스 제목</h1>
                            <p>뉴스 본문 내용입니다.</p>
                        </body>
                    </html>
                """.trimIndent()

                val message = ContentProcessedMessage(
                    id = "news-123",
                    type = SourceType.NEWS,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "articleId" to "article123",
                        "title" to "뉴스 제목",
                        "content" to htmlContent
                    )
                )

                val parseResult = DocumentParseResult(
                    extractedText = "뉴스 제목\n뉴스 본문 내용입니다.",
                    contentType = "text/html",
                    metadata = emptyMap(),
                    detectedLanguage = "ko",
                    success = true,
                    errorMessage = null
                )

                every {
                    tikaDocumentParser.parseDocument(any<InputStream>(), "text/html")
                } returns parseResult

                justRun {
                    textKafkaService.sendTextExtracted(any())
                }

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleNewsContentProcessed(messageJson, acknowledgment)

                // then
                verify(exactly = 1) {
                    tikaDocumentParser.parseDocument(any<InputStream>(), "text/html")
                }
                verify(exactly = 1) {
                    textKafkaService.sendTextExtracted(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("content가 없는 메시지를 수신했을 때") {
            it("에러를 처리하고 커밋하지 않아야 한다") {
                // given
                val message = ContentProcessedMessage(
                    id = "news-456",
                    type = SourceType.NEWS,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "articleId" to "article456",
                        "title" to "뉴스 제목"
                        // content 필드 없음
                    )
                )

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleNewsContentProcessed(messageJson, acknowledgment)

                // then
                verify(exactly = 0) {
                    tikaDocumentParser.parseDocument(any<InputStream>(), any())
                }
                verify(exactly = 0) {
                    acknowledgment.acknowledge()
                }
            }
        }
    }
})
