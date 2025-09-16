package com.sandro.realtime.codex.controller

import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.dto.CreateDocumentRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/test/kafka")
@Tag(name = "Test Kafka", description = "카프카 테스트용 API")
class TestKafkaController(
    private val kafkaTemplate: KafkaTemplate<String, CreateDocumentRequest>
) {
    private val logger = LoggerFactory.getLogger(TestKafkaController::class.java)

    @PostMapping("/publish/{type}")
    @Operation(summary = "테스트 문서 발행", description = "카프카로 테스트 문서를 발행합니다")
    fun publishTestDocument(
        @Parameter(description = "문서 타입") @PathVariable type: DocumentType
    ): ResponseEntity<Map<String, String>> {
        val topic = "document-index-${type.name.lowercase()}"
        
        val testDocument = when (type) {
            DocumentType.NEWS -> CreateDocumentRequest(
                documentType = DocumentType.NEWS,
                title = "테스트 뉴스 - ${System.currentTimeMillis()}",
                content = "이것은 카프카 테스트용 뉴스 문서입니다. 현재 시간: ${LocalDateTime.now()}",
                source = "테스트 출처",
                author = "테스트 기자",
                categories = listOf("테스트", "뉴스"),
                tags = listOf("kafka", "test", "news"),
                url = "https://test.example.com/news/${System.currentTimeMillis()}"
            )
            
            DocumentType.WIKI -> CreateDocumentRequest(
                documentType = DocumentType.WIKI,
                title = "테스트 위키 - ${System.currentTimeMillis()}",
                content = "이것은 카프카 테스트용 위키 문서입니다.\n\n## 개요\n테스트를 위한 위키 페이지입니다.\n\n## 내용\n- 카프카 메시지 테스트\n- 문서 색인 테스트",
                source = "wikipedia",
                author = "테스트 편집자",
                categories = listOf("테스트", "위키"),
                tags = listOf("kafka", "test", "wiki"),
                url = "https://test.example.com/wiki/${System.currentTimeMillis()}"
            )
            
            DocumentType.TWEET -> CreateDocumentRequest(
                documentType = DocumentType.TWEET,
                title = null,
                content = "카프카 테스트용 트윗입니다! 🚀 현재 시간: ${LocalDateTime.now()} #kafka #test #tweet",
                source = "twitter",
                publishedAt = LocalDateTime.now(),
                author = "@test_user",
                categories = listOf("테스트", "소셜미디어"),
                tags = listOf("kafka", "test", "tweet"),
                url = "https://twitter.com/test_user/status/${System.currentTimeMillis()}"
            )
        }

        try {
            kafkaTemplate.send(topic, testDocument)
            logger.info("테스트 문서 발행 완료: topic=$topic, type=$type")
            
            return ResponseEntity.ok(mapOf(
                "status" to "success",
                "topic" to topic,
                "type" to type.name,
                "title" to (testDocument.title ?: "제목 없음")
            ))
        } catch (e: Exception) {
            logger.error("테스트 문서 발행 실패: topic=$topic, type=$type", e)
            return ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to e.message.orEmpty()
            ))
        }
    }

    @PostMapping("/publish/batch")
    @Operation(summary = "배치 테스트 문서 발행", description = "모든 타입의 테스트 문서를 한 번에 발행합니다")
    fun publishBatchTestDocuments(): ResponseEntity<Map<String, Any>> {
        val results = mutableListOf<Map<String, String>>()
        
        DocumentType.entries.forEach { type ->
            try {
                val response = publishTestDocument(type)
                results.add(response.body!!)
            } catch (e: Exception) {
                logger.error("배치 발행 중 오류: type=$type", e)
                results.add(mapOf(
                    "status" to "error",
                    "type" to type.name,
                    "message" to e.message.orEmpty()
                ))
            }
        }
        
        return ResponseEntity.ok(mapOf(
            "status" to "completed",
            "results" to results,
            "total" to DocumentType.entries.size
        ))
    }
    
    @PostMapping("/publish/batch/{type}/{count}")
    @Operation(summary = "배치 크기별 테스트 문서 발행", description = "지정된 타입의 문서를 지정된 개수만큼 발행합니다")
    fun publishBatchDocumentsByCount(
        @Parameter(description = "문서 타입") @PathVariable type: DocumentType,
        @Parameter(description = "발행할 문서 개수") @PathVariable count: Int
    ): ResponseEntity<Map<String, Any>> {
        if (count <= 0 || count > 1000) {
            return ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "count는 1-1000 사이여야 합니다"
            ))
        }
        
        val topic = "document-index-${type.name.lowercase()}"
        val documents = mutableListOf<CreateDocumentRequest>()
        val timestamp = System.currentTimeMillis()
        
        repeat(count) { index ->
            val document = when (type) {
                DocumentType.NEWS -> CreateDocumentRequest(
                    documentType = DocumentType.NEWS,
                    title = "배치 테스트 뉴스 #${index + 1} - $timestamp",
                    content = "배치 테스트용 뉴스 문서입니다. 순번: ${index + 1}, 시간: ${LocalDateTime.now()}",
                    source = "배치테스트",
                    author = "배치테스터",
                    categories = listOf("테스트", "배치"),
                    tags = listOf("batch", "test", "news", "item-${index + 1}"),
                    url = "https://test.example.com/batch-news/$timestamp/${index + 1}"
                )
                
                DocumentType.WIKI -> CreateDocumentRequest(
                    documentType = DocumentType.WIKI,
                    title = "배치 테스트 위키 #${index + 1} - $timestamp",
                    content = "배치 테스트용 위키 문서입니다.\n\n## 순번\n${index + 1}\n\n## 시간\n${LocalDateTime.now()}\n\n## 내용\n배치 처리 테스트를 위한 문서입니다.",
                    source = "test-wiki",
                    author = "배치편집자",
                    categories = listOf("테스트", "배치", "위키"),
                    tags = listOf("batch", "test", "wiki", "item-${index + 1}"),
                    url = "https://test.example.com/batch-wiki/$timestamp/${index + 1}"
                )
                
                DocumentType.TWEET -> CreateDocumentRequest(
                    documentType = DocumentType.TWEET,
                    title = null,
                    content = "배치 테스트 트윗 #${index + 1} 🚀 시간: ${LocalDateTime.now()} #batch #test #tweet #item${index + 1}",
                    source = "twitter",
                    publishedAt = LocalDateTime.now(),
                    author = "@batch_tester",
                    categories = listOf("테스트", "배치", "소셜"),
                    tags = listOf("batch", "test", "tweet", "item-${index + 1}"),
                    url = "https://twitter.com/batch_tester/status/$timestamp${index + 1}"
                )
            }
            documents.add(document)
        }
        
        try {
            documents.forEach { document ->
                kafkaTemplate.send(topic, document)
            }
            
            logger.info("배치 문서 발행 완료: topic=$topic, type=$type, count=$count")
            
            return ResponseEntity.ok(mapOf(
                "status" to "success",
                "topic" to topic,
                "type" to type.name,
                "count" to count,
                "timestamp" to timestamp
            ))
        } catch (e: Exception) {
            logger.error("배치 문서 발행 실패: topic=$topic, type=$type, count=$count", e)
            return ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to e.message.orEmpty()
            ))
        }
    }
}