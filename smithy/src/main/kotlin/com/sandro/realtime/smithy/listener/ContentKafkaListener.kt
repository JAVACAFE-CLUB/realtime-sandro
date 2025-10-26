package com.sandro.realtime.smithy.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.common.message.TextExtractedMessage
import com.sandro.realtime.smithy.document.service.TikaDocumentParser
import com.sandro.realtime.smithy.kafka.TextKafkaService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

/**
 * harvest 모듈에서 발행한 콘텐츠 처리 완료 메시지를 수신하는 리스너
 *
 * 현재는 메시지를 수신하여 로깅만 수행하며, 향후 문서 파싱, MongoDB 저장 등의 기능을 추가할 수 있습니다.
 */
@Service
class ContentKafkaListener(
    private val tikaDocumentParser: TikaDocumentParser,
    private val textKafkaService: TextKafkaService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(ContentKafkaListener::class.java)

    /**
     * Wikipedia 콘텐츠 처리 완료 메시지 수신
     *
     * @param messageJson harvest 모듈에서 발행한 Wikipedia 콘텐츠 메시지 (JSON String)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.WIKI_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleWikiContentProcessed(
        messageJson: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            // JSON String을 ContentProcessedMessage로 역직렬화
            val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

            // revision.text에서 위키마크업 텍스트 추출
            val revision = message.content["revision"] as? Map<*, *>
            val wikiText = revision?.get("text") as? String

            if (wikiText == null) {
                logger.warn("Wiki 메시지에 revision.text가 없음: id=${message.id}")
                // 텍스트가 없으면 커밋하지 않아 재처리됨
                // FIXME: 예외처리
                return
            }

            // Tika로 텍스트 파싱 (use 블록으로 자동 리소스 관리)
            ByteArrayInputStream(wikiText.toByteArray(Charsets.UTF_8)).use { inputStream ->
                val parseResult = tikaDocumentParser.parseDocument(inputStream, "text/plain")

                if (parseResult.success) {
                    logger.info("Wiki 문서 파싱 성공: id=${message.id}")
                    logger.info("  - 원본 텍스트 길이: ${wikiText.length}자")
                    logger.info("  - 추출된 텍스트 길이: ${parseResult.extractedText.length}자")
                    logger.info("  - 감지된 언어: ${parseResult.detectedLanguage}")
                    logger.debug("추출된 전체 텍스트:\n${parseResult.extractedText}")

                    // 텍스트 추출 완료 메시지 발송
                    val textExtractedMessage = TextExtractedMessage(
                        sourceId = message.id,
                        sourceType = message.type,
                        extractedText = parseResult.extractedText,
                        extractedAt = LocalDateTime.now()
                    )
                    textKafkaService.sendTextExtracted(textExtractedMessage)
                } else {
                    logger.warn("Wiki 문서 파싱 실패: id=${message.id}, error=${parseResult.errorMessage}")
                }
            }

            // 수동 커밋
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Wiki 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }

    /**
     * News 콘텐츠 처리 완료 메시지 수신
     *
     * @param messageJson harvest 모듈에서 발행한 News 콘텐츠 메시지 (JSON String)
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
        topics = [KafkaTopic.NEWS_CONTENT_PROCESSED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleNewsContentProcessed(
        messageJson: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            // JSON String을 ContentProcessedMessage로 역직렬화
            val message: ContentProcessedMessage = objectMapper.readValue(messageJson)

            logger.debug("News 콘텐츠 메시지 수신: id=${message.id}, type=${message.type}, processedAt=${message.processedAt}")
            logger.debug("콘텐츠 상세: ${message.content}")

            // content에서 HTML 텍스트 추출
            val htmlContent = message.content["content"] as? String

            if (htmlContent == null) {
                logger.warn("News 메시지에 content가 없음: id=${message.id}")
                // 텍스트가 없으면 커밋하지 않아 재처리됨
                return
            }

            // Tika로 HTML 파싱 (use 블록으로 자동 리소스 관리)
            ByteArrayInputStream(htmlContent.toByteArray(Charsets.UTF_8)).use { inputStream ->
                val parseResult = tikaDocumentParser.parseDocument(inputStream, "text/html")

                if (parseResult.success) {
                    logger.info("News 문서 파싱 성공: id=${message.id}")
                    logger.info("  - 원본 HTML 길이: ${htmlContent.length}자")
                    logger.info("  - 추출된 텍스트 길이: ${parseResult.extractedText.length}자")
                    logger.info("  - 감지된 언어: ${parseResult.detectedLanguage}")
                    logger.debug("추출된 전체 텍스트:\n${parseResult.extractedText}")

                    // 텍스트 추출 완료 메시지 발송
                    val textExtractedMessage = TextExtractedMessage(
                        sourceId = message.id,
                        sourceType = message.type,
                        extractedText = parseResult.extractedText,
                        extractedAt = LocalDateTime.now()
                    )
                    textKafkaService.sendTextExtracted(textExtractedMessage)
                } else {
                    logger.warn("News 문서 파싱 실패: id=${message.id}, error=${parseResult.errorMessage}")
                }
            }

            // 수동 커밋
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("News 콘텐츠 처리 중 예외 발생: error=${e.message}", e)
            // 에러 발생 시 커밋하지 않아 재처리됨
        }
    }
}
