package com.sandro.realtime.smithy.service

import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.common.message.TextExtractedMessage
import com.sandro.realtime.smithy.document.ExtractedContent
import com.sandro.realtime.smithy.document.repository.ExtractedContentRepository
import com.sandro.realtime.smithy.document.service.TikaDocumentParser
import com.sandro.realtime.smithy.kafka.TextKafkaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

/**
 * 콘텐츠 처리 서비스
 *
 * harvest 모듈에서 수신한 콘텐츠를 파싱하고, fulltext를 추출하여
 * MongoDB에 저장하고 Kafka로 전송하는 비즈니스 로직을 담당합니다.
 */
@Service
class ContentProcessingService(
    private val tikaDocumentParser: TikaDocumentParser,
    private val textKafkaService: TextKafkaService,
    private val extractedContentRepository: ExtractedContentRepository
) {

    private val logger = LoggerFactory.getLogger(ContentProcessingService::class.java)

    /**
     * Wikipedia 콘텐츠 처리
     *
     * @param message harvest 모듈에서 발행한 Wikipedia 콘텐츠 메시지
     * @throws IllegalArgumentException revision.text가 없는 경우
     */
    fun processWikiContent(message: ContentProcessedMessage) {
        // revision.text에서 위키마크업 텍스트 추출
        val revision = message.content["revision"] as? Map<*, *>
            ?: throw IllegalArgumentException("Wiki 메시지에 revision이 없음: id=${message.id}")

        val wikiText = revision["text"] as? String
            ?: throw IllegalArgumentException("Wiki 메시지에 revision.text가 없음: id=${message.id}")

        logger.info("Wiki 콘텐츠 처리 시작: id=${message.id}, 원본 텍스트 길이=${wikiText.length}자")

        // Tika로 텍스트 파싱
        ByteArrayInputStream(wikiText.toByteArray(Charsets.UTF_8)).use { inputStream ->
            val parseResult = tikaDocumentParser.parseDocument(inputStream, "text/plain")

            if (parseResult.success) {
                logger.info("Wiki 문서 파싱 성공: id=${message.id}")
                logger.info("  - 원본 텍스트 길이: ${wikiText.length}자")
                logger.info("  - 추출된 텍스트 길이: ${parseResult.extractedText.length}자")
                logger.info("  - 감지된 언어: ${parseResult.detectedLanguage}")
                logger.debug("추출된 전체 텍스트:\n${parseResult.extractedText}")

                // MongoDB 저장 및 Kafka 메시지 발송
                saveAndSendExtractedText(
                    sourceId = message.id,
                    sourceType = message.type,
                    extractedText = parseResult.extractedText,
                    originalLength = wikiText.length,
                    detectedLanguage = parseResult.detectedLanguage
                )
            } else {
                logger.warn("Wiki 문서 파싱 실패: id=${message.id}, error=${parseResult.errorMessage}")
                throw IllegalStateException("Wiki 문서 파싱 실패: ${parseResult.errorMessage}")
            }
        }
    }

    /**
     * News 콘텐츠 처리
     *
     * @param message harvest 모듈에서 발행한 News 콘텐츠 메시지
     * @throws IllegalArgumentException content가 없는 경우
     */
    fun processNewsContent(message: ContentProcessedMessage) {
        logger.debug("News 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}, processedAt=${message.processedAt}")
        logger.debug("콘텐츠 상세: ${message.content}")

        // content에서 HTML 텍스트 추출
        val htmlContent = message.content["content"] as? String
            ?: throw IllegalArgumentException("News 메시지에 content가 없음: id=${message.id}")

        logger.info("News 콘텐츠 처리 시작: id=${message.id}, 원본 HTML 길이=${htmlContent.length}자")

        // Tika로 HTML 파싱
        ByteArrayInputStream(htmlContent.toByteArray(Charsets.UTF_8)).use { inputStream ->
            val parseResult = tikaDocumentParser.parseDocument(inputStream, "text/html")

            if (parseResult.success) {
                logger.info("News 문서 파싱 성공: id=${message.id}")
                logger.info("  - 원본 HTML 길이: ${htmlContent.length}자")
                logger.info("  - 추출된 텍스트 길이: ${parseResult.extractedText.length}자")
                logger.info("  - 감지된 언어: ${parseResult.detectedLanguage}")
                logger.debug("추출된 전체 텍스트:\n${parseResult.extractedText}")

                // MongoDB 저장 및 Kafka 메시지 발송
                saveAndSendExtractedText(
                    sourceId = message.id,
                    sourceType = message.type,
                    extractedText = parseResult.extractedText,
                    originalLength = htmlContent.length,
                    detectedLanguage = parseResult.detectedLanguage
                )
            } else {
                logger.warn("News 문서 파싱 실패: id=${message.id}, error=${parseResult.errorMessage}")
                throw IllegalStateException("News 문서 파싱 실패: ${parseResult.errorMessage}")
            }
        }
    }

    /**
     * 추출된 텍스트를 MongoDB에 저장하고 Kafka로 전송
     *
     * @param sourceId 원본 콘텐츠 ID
     * @param sourceType 소스 타입 (WIKI, NEWS)
     * @param extractedText 추출된 fulltext
     * @param originalLength 원본 텍스트 길이
     * @param detectedLanguage 감지된 언어
     */
    private fun saveAndSendExtractedText(
        sourceId: String,
        sourceType: SourceType,
        extractedText: String,
        originalLength: Int,
        detectedLanguage: String?
    ) {
        val extractedAt = LocalDateTime.now()

        // MongoDB에 저장
        val extractedContent = ExtractedContent(
            sourceId = sourceId,
            sourceType = sourceType,
            extractedText = extractedText,
            extractedAt = extractedAt,
            originalLength = originalLength,
            detectedLanguage = detectedLanguage
        )

        val savedContent = extractedContentRepository.save(extractedContent)
        logger.info("MongoDB 저장 완료: id=${savedContent.id}, sourceId=${sourceId}")

        // 텍스트 추출 완료 메시지 발송
        val textExtractedMessage = TextExtractedMessage(
            sourceId = sourceId,
            sourceType = sourceType,
            extractedText = extractedText,
            extractedAt = extractedAt
        )
        // FIXME: ApplicationEvent로 변경
        textKafkaService.sendTextExtracted(textExtractedMessage)
        logger.info("Kafka 메시지 발송 완료: sourceId=${sourceId}")
    }
}
