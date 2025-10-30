package com.sandro.realtime.smithy.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.smithy.service.ContentProcessingService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime

/**
 * ContentKafkaListener 단위 테스트
 *
 * Kafka 메시지 수신 및 ContentProcessingService 호출을 검증합니다.
 */
class ContentKafkaListenerUnitTest : DescribeSpec({

    lateinit var contentProcessingService: ContentProcessingService
    lateinit var objectMapper: ObjectMapper
    lateinit var contentKafkaListener: ContentKafkaListener
    lateinit var acknowledgment: Acknowledgment

    beforeEach {
        contentProcessingService = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        contentKafkaListener = ContentKafkaListener(contentProcessingService, objectMapper)
        acknowledgment = mockk(relaxed = true)
    }

    describe("handleWikiContentProcessed") {
        context("Wikipedia 메시지를 수신했을 때") {
            it("ContentProcessingService를 호출하고 커밋해야 한다") {
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

                justRun {
                    contentProcessingService.processWikiContent(any())
                }

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleWikiContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processWikiContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("revision.text가 없는 메시지를 수신했을 때") {
            it("IllegalArgumentException을 처리하고 커밋해야 한다") {
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

                every {
                    contentProcessingService.processWikiContent(any())
                } throws IllegalArgumentException("Wiki 메시지에 revision.text가 없음")

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleWikiContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processWikiContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("처리 중 예외가 발생했을 때") {
            it("예외를 로깅하고 배치 전체를 커밋해야 한다") {
                // given
                val message = ContentProcessedMessage(
                    id = "wiki-789",
                    type = SourceType.WIKIPEDIA,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "id" to 12345L,
                        "title" to "테스트 페이지",
                        "revision" to mapOf(
                            "id" to 67890L,
                            "text" to "test content"
                        )
                    )
                )

                every {
                    contentProcessingService.processWikiContent(any())
                } throws RuntimeException("처리 중 예외 발생")

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleWikiContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processWikiContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }
    }

    describe("handleNewsContentProcessed") {
        context("News 메시지를 수신했을 때") {
            it("ContentProcessingService를 호출하고 커밋해야 한다") {
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

                justRun {
                    contentProcessingService.processNewsContent(any())
                }

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleNewsContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processNewsContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("content가 없는 메시지를 수신했을 때") {
            it("IllegalArgumentException을 처리하고 커밋해야 한다") {
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

                every {
                    contentProcessingService.processNewsContent(any())
                } throws IllegalArgumentException("News 메시지에 content가 없음")

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleNewsContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processNewsContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }

        context("처리 중 예외가 발생했을 때") {
            it("예외를 로깅하고 배치 전체를 커밋해야 한다") {
                // given
                val message = ContentProcessedMessage(
                    id = "news-789",
                    type = SourceType.NEWS,
                    processedAt = LocalDateTime.now(),
                    content = mapOf(
                        "articleId" to "article789",
                        "title" to "뉴스 제목",
                        "content" to "<html>test</html>"
                    )
                )

                every {
                    contentProcessingService.processNewsContent(any())
                } throws RuntimeException("처리 중 예외 발생")

                // when
                val messageJson = objectMapper.writeValueAsString(message)
                contentKafkaListener.handleNewsContentProcessed(listOf(messageJson), acknowledgment)

                // then
                verify(exactly = 1) {
                    contentProcessingService.processNewsContent(any())
                }
                verify(exactly = 1) {
                    acknowledgment.acknowledge()
                }
            }
        }
    }
})
