package com.sandro.realtime.codex.controller

import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.dto.CreateDocumentRequest
import com.sandro.realtime.codex.dto.DocumentResponse
import com.sandro.realtime.codex.dto.DocumentSearchRequest
import com.sandro.realtime.codex.service.DocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document", description = "통합 문서 관리 API (위키페이지, 뉴스, 트윗)")
class DocumentController(
    private val documentService: DocumentService
) {

    @PostMapping
    @Operation(summary = "문서 생성", description = "새로운 문서를 생성합니다 (위키페이지, 뉴스, 트윗)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    fun createDocument(
        @Valid @RequestBody request: CreateDocumentRequest
    ): ResponseEntity<DocumentResponse> {
        val response = documentService.createDocument(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/batch")
    @Operation(summary = "문서 배치 생성", description = "여러 문서를 한번에 생성합니다")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    fun createDocumentsBatch(
        @Valid @RequestBody requests: List<CreateDocumentRequest>
    ): ResponseEntity<List<DocumentResponse>> {
        val responses = documentService.createDocumentsBatch(requests)
        return ResponseEntity.status(HttpStatus.CREATED).body(responses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "문서 조회", description = "ID로 문서를 조회합니다")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "문서를 찾을 수 없음")
    )
    fun getDocument(
        @Parameter(description = "문서 ID") @PathVariable id: String
    ): ResponseEntity<DocumentResponse> {
        val response = documentService.getDocument(id)
        return response?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/search")
    @Operation(summary = "문서 검색", description = "제목 또는 내용으로 문서를 검색합니다")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검색 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    fun searchDocuments(
        @Valid @ModelAttribute request: DocumentSearchRequest
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchDocuments(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/type/{documentType}")
    @Operation(summary = "문서 타입별 조회", description = "문서 타입(WIKI, NEWS, TWEET)별로 조회합니다")
    fun getDocumentsByType(
        @Parameter(description = "문서 타입") @PathVariable documentType: DocumentType,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByDocumentType(documentType, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/title")
    @Operation(summary = "제목으로 검색", description = "제목에 포함된 키워드로 문서를 검색합니다")
    fun searchByTitle(
        @Parameter(description = "검색할 제목") @RequestParam title: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByTitle(title, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/content")
    @Operation(summary = "내용으로 검색", description = "내용에 포함된 키워드로 문서를 검색합니다")
    fun searchByContent(
        @Parameter(description = "검색할 내용") @RequestParam content: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByContent(content, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/category")
    @Operation(summary = "카테고리로 검색", description = "카테고리로 문서를 검색합니다")
    fun searchByCategory(
        @Parameter(description = "카테고리") @RequestParam category: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByCategory(category, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/source")
    @Operation(summary = "소스로 검색", description = "소스로 문서를 검색합니다")
    fun searchBySource(
        @Parameter(description = "소스") @RequestParam source: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchBySource(source, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/author")
    @Operation(summary = "작성자로 검색", description = "작성자로 문서를 검색합니다")
    fun searchByAuthor(
        @Parameter(description = "작성자") @RequestParam author: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByAuthor(author, page, size)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search/tag")
    @Operation(summary = "태그로 검색", description = "태그로 문서를 검색합니다")
    fun searchByTag(
        @Parameter(description = "태그") @RequestParam tag: String,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<DocumentResponse>> {
        val response = documentService.searchByTag(tag, page, size)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "문서 삭제", description = "ID로 문서를 삭제합니다")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(responseCode = "404", description = "문서를 찾을 수 없음")
    )
    fun deleteDocument(
        @Parameter(description = "문서 ID") @PathVariable id: String
    ): ResponseEntity<Void> {
        return if (documentService.deleteDocument(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/count")
    @Operation(summary = "문서 개수 조회", description = "전체 문서 개수를 조회합니다")
    fun count(): ResponseEntity<Map<String, Long>> {
        val count = documentService.count()
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @GetMapping("/count/{documentType}")
    @Operation(summary = "문서 타입별 개수 조회", description = "문서 타입별 개수를 조회합니다")
    fun countByType(
        @Parameter(description = "문서 타입") @PathVariable documentType: DocumentType
    ): ResponseEntity<Map<String, Any>> {
        val count = documentService.countByDocumentType(documentType)
        return ResponseEntity.ok(mapOf("count" to count, "type" to documentType.name))
    }
}