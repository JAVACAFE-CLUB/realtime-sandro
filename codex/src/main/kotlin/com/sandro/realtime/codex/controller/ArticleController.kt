package com.sandro.realtime.codex.controller

import com.sandro.realtime.codex.dto.ArticleResponse
import com.sandro.realtime.codex.dto.CreateArticleRequest
import com.sandro.realtime.codex.dto.UpdateArticleRequest
import com.sandro.realtime.codex.service.ArticleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Article", description = "게시글 관리 API")
@RestController
@RequestMapping("/api/articles")
class ArticleController(
    private val articleService: ArticleService
) {

    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "게시글 생성 성공",
                content = [Content(schema = Schema(implementation = ArticleResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "잘못된 요청")
        ]
    )
    @PostMapping
    fun createArticle(
        @Valid @RequestBody request: CreateArticleRequest
    ): ResponseEntity<ArticleResponse> {
        val article = articleService.create(request.title, request.content)
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.from(article))
    }

    @Operation(summary = "게시글 조회", description = "ID로 특정 게시글을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "게시글 조회 성공",
                content = [Content(schema = Schema(implementation = ArticleResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
        ]
    )
    @GetMapping("/{id}")
    fun getArticle(
        @Parameter(description = "게시글 ID", required = true) @PathVariable id: String
    ): ResponseEntity<ArticleResponse> {
        val article = articleService.findById(id)
        return if (article != null) {
            ResponseEntity.ok(ArticleResponse.from(article))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "전체 게시글 조회", description = "모든 게시글을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "게시글 목록 조회 성공",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ArticleResponse::class)))]
    )
    @GetMapping
    fun getAllArticles(): ResponseEntity<List<ArticleResponse>> {
        val articles = articleService.findAll()
        return ResponseEntity.ok(articles.map { ArticleResponse.from(it) })
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "게시글 수정 성공",
                content = [Content(schema = Schema(implementation = ArticleResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
        ]
    )
    @PutMapping("/{id}")
    fun updateArticle(
        @Parameter(description = "게시글 ID", required = true) @PathVariable id: String,
        @RequestBody request: UpdateArticleRequest
    ): ResponseEntity<ArticleResponse> {
        val article = articleService.update(id, request.title, request.content)
        return if (article != null) {
            ResponseEntity.ok(ArticleResponse.from(article))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
        ]
    )
    @DeleteMapping("/{id}")
    fun deleteArticle(
        @Parameter(description = "게시글 ID", required = true) @PathVariable id: String
    ): ResponseEntity<Void> {
        return if (articleService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "제목으로 게시글 검색", description = "제목에 키워드가 포함된 게시글을 검색합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "검색 성공",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ArticleResponse::class)))]
    )
    @GetMapping("/search/title")
    fun searchByTitle(
        @Parameter(description = "검색 키워드", required = true) @RequestParam keyword: String
    ): ResponseEntity<List<ArticleResponse>> {
        val articles = articleService.searchByTitle(keyword)
        return ResponseEntity.ok(articles.map { ArticleResponse.from(it) })
    }

    @Operation(summary = "내용으로 게시글 검색", description = "내용에 키워드가 포함된 게시글을 검색합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "검색 성공",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ArticleResponse::class)))]
    )
    @GetMapping("/search/content")
    fun searchByContent(
        @Parameter(description = "검색 키워드", required = true) @RequestParam keyword: String
    ): ResponseEntity<List<ArticleResponse>> {
        val articles = articleService.searchByContent(keyword)
        return ResponseEntity.ok(articles.map { ArticleResponse.from(it) })
    }
}