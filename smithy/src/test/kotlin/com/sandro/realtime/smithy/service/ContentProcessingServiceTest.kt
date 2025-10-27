package com.sandro.realtime.smithy.service

import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.smithy.document.ExtractedContent
import com.sandro.realtime.smithy.document.repository.ExtractedContentRepository
import com.sandro.realtime.smithy.document.service.DocumentParseResult
import com.sandro.realtime.smithy.document.service.TikaDocumentParser
import com.sandro.realtime.smithy.event.TextExtractedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

/**
 * ContentProcessingService 단위 테스트
 *
 * ApplicationEvent를 사용한 콘텐츠 처리 로직을 검증합니다.
 */
class ContentProcessingServiceTest : DescribeSpec({

    lateinit var tikaDocumentParser: TikaDocumentParser
    lateinit var eventPublisher: ApplicationEventPublisher
    lateinit var extractedContentRepository: ExtractedContentRepository
    lateinit var contentProcessingService: ContentProcessingService

    beforeEach {
        tikaDocumentParser = mockk()
        eventPublisher = mockk()
        extractedContentRepository = mockk()
        contentProcessingService = ContentProcessingService(
            tikaDocumentParser,
            eventPublisher,
            extractedContentRepository
        )
    }

    describe("processWikiContent") {
        context("정상적인 Wikipedia 메시지를 처리할 때") {
            it("텍스트를 추출하고 MongoDB에 저장한 후 이벤트를 발행해야 한다") {
                // given
                val wikiText = """
                    == 개요 ==
                    테스트 위키 페이지입니다.

                    == 상세 ==
                    상세 내용이 여기에 들어갑니다.
                """.trimIndent()

                val extractedText = "개요 테스트 위키 페이지입니다. 상세 상세 내용이 여기에 들어갑니다."

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
                    extractedText = extractedText,
                    contentType = "text/plain",
                    metadata = emptyMap(),
                    detectedLanguage = "ko",
                    success = true,
                    errorMessage = null
                )

                val savedContent = ExtractedContent(
                    id = "extracted-123",
                    sourceId = message.id,
                    sourceType = message.type,
                    extractedText = extractedText,
                    extractedAt = LocalDateTime.now(),
                    originalLength = wikiText.length,
                    detectedLanguage = "ko"
                )

                every {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/plain")
                } returns parseResult

                every {
                    extractedContentRepository.save(any())
                } returns savedContent

                every {
                    eventPublisher.publishEvent(any<TextExtractedEvent>())
                } just Runs

                // when
                contentProcessingService.processWikiContent(message)

                // then
                verify(exactly = 1) {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/plain")
                }

                verify(exactly = 1) {
                    extractedContentRepository.save(match {
                        it.sourceId == message.id &&
                                it.sourceType == message.type &&
                                it.extractedText == extractedText &&
                                it.originalLength == wikiText.length &&
                                it.detectedLanguage == "ko"
                    })
                }

                verify(exactly = 1) {
                    eventPublisher.publishEvent(match<TextExtractedEvent> {
                        it.message.sourceId == message.id &&
                                it.message.sourceType == message.type &&
                                it.message.extractedText == extractedText
                    })
                }
            }
        }

        context("revision.text가 없는 메시지를 받았을 때") {
            it("IllegalArgumentException을 던져야 한다") {
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

                // when & then
                val exception = shouldThrow<IllegalArgumentException> {
                    contentProcessingService.processWikiContent(message)
                }

                exception.message shouldBe "Wiki 메시지에 revision.text가 없음: id=wiki-456"
            }
        }

        context("파싱에 실패했을 때") {
            it("IllegalStateException을 던져야 한다") {
                // given
                val wikiText = "테스트 텍스트"
                val message = ContentProcessedMessage(
                    id = "wiki-789",
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
                    extractedText = "",
                    contentType = "text/plain",
                    metadata = emptyMap(),
                    detectedLanguage = null,
                    success = false,
                    errorMessage = "파싱 실패"
                )

                every {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/plain")
                } returns parseResult

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    contentProcessingService.processWikiContent(message)
                }

                exception.message shouldBe "Wiki 문서 파싱 실패: 파싱 실패"
            }
        }
    }

    describe("processNewsContent") {
        context("정상적인 News 메시지를 처리할 때") {
            it("HTML을 파싱하고 MongoDB에 저장한 후 이벤트를 발행해야 한다") {
                // given
                val htmlContent = """
                    <html>
                        <body>
                            <h1>뉴스 제목</h1>
                            <p>뉴스 본문 내용입니다.</p>
                        </body>
                    </html>
                """.trimIndent()

                val extractedText = "뉴스 제목 뉴스 본문 내용입니다."

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
                    extractedText = extractedText,
                    contentType = "text/html",
                    metadata = emptyMap(),
                    detectedLanguage = "ko",
                    success = true,
                    errorMessage = null
                )

                val savedContent = ExtractedContent(
                    id = "extracted-456",
                    sourceId = message.id,
                    sourceType = message.type,
                    extractedText = extractedText,
                    extractedAt = LocalDateTime.now(),
                    originalLength = htmlContent.length,
                    detectedLanguage = "ko"
                )

                every {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/html")
                } returns parseResult

                every {
                    extractedContentRepository.save(any())
                } returns savedContent

                every {
                    eventPublisher.publishEvent(any<TextExtractedEvent>())
                } just Runs

                // when
                contentProcessingService.processNewsContent(message)

                // then
                verify(exactly = 1) {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/html")
                }

                verify(exactly = 1) {
                    extractedContentRepository.save(match {
                        it.sourceId == message.id &&
                                it.sourceType == message.type &&
                                it.extractedText == extractedText &&
                                it.originalLength == htmlContent.length &&
                                it.detectedLanguage == "ko"
                    })
                }

                verify(exactly = 1) {
                    eventPublisher.publishEvent(match<TextExtractedEvent> {
                        it.message.sourceId == message.id &&
                                it.message.sourceType == message.type &&
                                it.message.extractedText == extractedText
                    })
                }
            }
        }

        context("content가 없는 메시지를 받았을 때") {
            it("IllegalArgumentException을 던져야 한다") {
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

                // when & then
                val exception = shouldThrow<IllegalArgumentException> {
                    contentProcessingService.processNewsContent(message)
                }

                exception.message shouldBe "News 메시지에 content가 없음: id=news-456"
            }
        }

        context("파싱에 실패했을 때") {
            it("IllegalStateException을 던져야 한다") {
                // given
                val htmlContent = "<html>test</html>"
                val message = ContentProcessedMessage(
                    id = "news-789",
                    type = SourceType.NEWS,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "articleId" to "article789",
                        "title" to "뉴스 제목",
                        "content" to htmlContent
                    )
                )

                val parseResult = DocumentParseResult(
                    extractedText = "",
                    contentType = "text/html",
                    metadata = emptyMap(),
                    detectedLanguage = null,
                    success = false,
                    errorMessage = "파싱 실패"
                )

                every {
                    tikaDocumentParser.parseDocument(any<ByteArrayInputStream>(), "text/html")
                } returns parseResult

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    contentProcessingService.processNewsContent(message)
                }

                exception.message shouldBe "News 문서 파싱 실패: 파싱 실패"
            }
        }
    }
})
