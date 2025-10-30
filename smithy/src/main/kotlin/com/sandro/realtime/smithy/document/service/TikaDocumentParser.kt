package com.sandro.realtime.smithy.document.service

import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream

/**
 * Apache Tika를 사용한 문서 파싱 서비스
 *
 * 다양한 포맷의 문서에서 텍스트와 메타데이터를 추출합니다.
 * - PDF, DOCX, XLSX, PPTX
 * - HTML, XML, TXT
 * - 이미지 (OCR 미지원)
 */
@Service
class TikaDocumentParser {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val tika = Tika()
    private val parser: Parser = AutoDetectParser()

    /**
     * InputStream에서 문서 파싱
     *
     * @param inputStream 문서 입력 스트림
     * @param providedContentType 제공된 컨텐츠 타입 (null이면 자동 감지)
     * @return 파싱 결과
     */
    fun parseDocument(
        inputStream: InputStream,
        providedContentType: String?
    ): DocumentParseResult {
        return try {
            // 메타데이터 추출을 위한 Metadata 객체
            val metadata = Metadata()
            if (providedContentType != null) {
                metadata.set(Metadata.CONTENT_TYPE, providedContentType)
            }

            // 텍스트 추출을 위한 핸들러 (-1은 텍스트 길이 제한 없음)
            val handler = BodyContentHandler(-1)
            val context = ParseContext()

            // 문서 파싱
            parser.parse(inputStream, handler, metadata, context)

            // 추출된 텍스트
            var extractedText = handler.toString().trim()

            // 위키마크업 감지 및 파싱
            if (isWikiMarkup(extractedText)) {
                logger.info("위키마크업 감지됨, Sweble Wikitext로 파싱 시작")
                extractedText = parseWikiMarkup(extractedText)
            }

            // 컨텐츠 타입 확인
            val detectedContentType = metadata.get(Metadata.CONTENT_TYPE) ?: "unknown"

            // 메타데이터 변환
            val metadataMap = metadata.names().associateWith { metadata.get(it) ?: "" }

            // 언어 감지
            val detectedLanguage = detectLanguage(extractedText)

            logger.info("문서 파싱 완료 - 컨텐츠 타입: $detectedContentType, 텍스트 길이: ${extractedText.length}")

            DocumentParseResult(
                extractedText = extractedText,
                contentType = detectedContentType,
                metadata = metadataMap,
                detectedLanguage = detectedLanguage,
                success = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            logger.error("문서 파싱 중 에러 발생", e)
            DocumentParseResult(
                extractedText = "",
                contentType = providedContentType ?: "unknown",
                metadata = emptyMap(),
                detectedLanguage = null,
                success = false,
                errorMessage = "파싱 에러: ${e.message}"
            )
        }
    }

    /**
     * 컨텐츠 타입 자동 감지
     *
     * @param inputStream 문서 입력 스트림
     * @return MIME 타입
     */
    fun detectContentType(inputStream: InputStream): String {
        return try {
            tika.detect(inputStream)
        } catch (e: Exception) {
            logger.warn("컨텐츠 타입 감지 실패", e)
            "application/octet-stream"
        }
    }

    /**
     * 텍스트의 언어 감지
     *
     * 현재는 간단한 휴리스틱 기반으로 한글/영어를 감지합니다.
     * 향후 Tika의 언어 감지 라이브러리를 추가하여 개선할 수 있습니다.
     *
     * @param text 감지할 텍스트
     * @return 언어 코드 (예: ko, en, ja)
     */
    private fun detectLanguage(text: String): String? {
        return try {
            if (text.isBlank()) return null

            // 간단한 휴리스틱: 한글 문자가 포함되어 있으면 한국어
            val koreanPattern = Regex("[ㄱ-ㅎ가-힣]")
            if (koreanPattern.containsMatchIn(text)) {
                return "ko"
            }

            // 영어 알파벳이 주를 이루면 영어
            val englishPattern = Regex("[a-zA-Z]")
            if (englishPattern.containsMatchIn(text)) {
                return "en"
            }

            null
        } catch (e: Exception) {
            logger.warn("언어 감지 실패", e)
            null
        }
    }

    /**
     * 위키마크업 여부 감지
     *
     * @param text 검사할 텍스트
     * @return 위키마크업이면 true
     */
    private fun isWikiMarkup(text: String): Boolean {
        // 위키마크업의 일반적인 패턴들
        val hasTemplate = text.contains("{{") && text.contains("}}")
        val hasLink = text.contains("[[") && text.contains("]]")
        val hasHeading = text.contains("===") || text.contains("==")
        val hasList = Regex("""^[*#]""", RegexOption.MULTILINE).containsMatchIn(text)
        val hasBold = text.contains("'''")
        val hasItalic = text.contains("''")

        return hasTemplate || hasLink || hasHeading || hasList || hasBold || hasItalic
    }

    /**
     * 위키마크업 파싱
     *
     * @param wikiText 위키마크업 텍스트
     * @return 플레인 텍스트
     */
    private fun parseWikiMarkup(wikiText: String): String {
        return try {
            // Bliki WikiModel 생성
            val wikiModel = WikiModel("", "")

            // 위키마크업을 플레인 텍스트로 변환
            val plainText = wikiModel.render(PlainTextConverter(), wikiText)

            logger.info("위키마크업 파싱 완료 - 원본: ${wikiText.length}자, 파싱: ${plainText.length}자")
            plainText

        } catch (e: Exception) {
            logger.error("위키마크업 파싱 실패, 원본 텍스트 반환", e)
            wikiText // 파싱 실패시 원본 반환
        }
    }
}

/**
 * 문서 파싱 결과
 */
data class DocumentParseResult(
    /**
     * 추출된 텍스트
     */
    val extractedText: String,

    /**
     * 문서 컨텐츠 타입
     */
    val contentType: String,

    /**
     * 문서 메타데이터
     */
    val metadata: Map<String, String>,

    /**
     * 감지된 언어
     */
    val detectedLanguage: String?,

    /**
     * 파싱 성공 여부
     */
    val success: Boolean,

    /**
     * 에러 메시지 (실패 시)
     */
    val errorMessage: String?
)
