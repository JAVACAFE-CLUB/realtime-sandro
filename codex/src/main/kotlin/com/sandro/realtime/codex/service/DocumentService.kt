package com.sandro.realtime.codex.service

import com.sandro.realtime.codex.domain.Document
import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.dto.CreateDocumentRequest
import com.sandro.realtime.codex.dto.DocumentResponse
import com.sandro.realtime.codex.dto.DocumentSearchRequest
import com.sandro.realtime.codex.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DocumentService(
    private val documentRepository: DocumentRepository
) {
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)

    @Transactional
    fun createDocument(request: CreateDocumentRequest): DocumentResponse {
        logger.info("문서 생성 요청: type=${request.documentType}, title=${request.title}")

        val document = Document(
            documentType = request.documentType,
            title = request.title,
            content = request.content,
            source = request.source,
            publishedAt = request.publishedAt,
            author = request.author,
            categories = request.categories,
            tags = request.tags,
            url = request.url,
            metadata = request.metadata
        )

        val savedDocument = documentRepository.save(document)
        logger.info("문서 생성 완료: id=${savedDocument.id}, type=${savedDocument.documentType}")

        return DocumentResponse.from(savedDocument)
    }

    @Transactional
    fun createDocumentsBatch(requests: List<CreateDocumentRequest>): List<DocumentResponse> {
        logger.info("문서 배치 생성 요청: ${requests.size}개")

        val documents = requests.map { request ->
            Document(
                documentType = request.documentType,
                title = request.title,
                content = request.content,
                source = request.source,
                publishedAt = request.publishedAt,
                author = request.author,
                categories = request.categories,
                tags = request.tags,
                url = request.url,
                metadata = request.metadata
            )
        }

        val savedDocuments = documentRepository.saveAll(documents)
        logger.info("문서 배치 생성 완료: ${savedDocuments.count()}개")

        return savedDocuments.map { DocumentResponse.from(it) }
    }

    fun getDocument(id: String): DocumentResponse? {
        logger.debug("문서 조회: id=$id")

        val document = documentRepository.findById(id).orElse(null)
        return document?.let { DocumentResponse.from(it) }
    }

    fun searchDocuments(request: DocumentSearchRequest): Page<DocumentResponse> {
        logger.info("문서 검색: query=${request.query}, type=${request.documentType}, category=${request.category}")

        val pageable = PageRequest.of(
            request.page,
            request.size,
            if (request.sortDirection.equals("ASC", ignoreCase = true)) {
                Sort.by(request.sortBy).ascending()
            } else {
                Sort.by(request.sortBy).descending()
            }
        )

        val result = when {
            request.documentType != null && request.category != null -> {
                documentRepository.searchByQueryAndDocumentTypeAndCategory(
                    request.query,
                    request.documentType,
                    request.category,
                    pageable
                )
            }

            request.documentType != null -> {
                documentRepository.searchByQueryAndDocumentType(
                    request.query,
                    request.documentType,
                    pageable
                )
            }

            request.category != null -> {
                documentRepository.searchByQueryAndCategory(
                    request.query,
                    request.category,
                    pageable
                )
            }

            else -> {
                documentRepository.searchByTitleOrContent(request.query, pageable)
            }
        }

        logger.info("문서 검색 결과: ${result.totalElements}개 문서 발견")

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByDocumentType(documentType: DocumentType, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("문서 타입별 검색: type=$documentType")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByDocumentType(documentType, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByTitle(title: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("제목으로 문서 검색: title=$title")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByTitleContaining(title, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByContent(content: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("내용으로 문서 검색: content=$content")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByContentContaining(content, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByCategory(category: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("카테고리로 문서 검색: category=$category")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByCategoriesContaining(category, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchBySource(source: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("소스로 문서 검색: source=$source")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findBySource(source, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByAuthor(author: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("작성자로 문서 검색: author=$author")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByAuthor(author, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    fun searchByTag(tag: String, page: Int = 0, size: Int = 20): Page<DocumentResponse> {
        logger.debug("태그로 문서 검색: tag=$tag")

        val pageable = PageRequest.of(page, size)
        val result = documentRepository.findByTagsContaining(tag, pageable)

        return result.map { DocumentResponse.from(it) }
    }

    @Transactional
    fun deleteDocument(id: String): Boolean {
        logger.info("문서 삭제: id=$id")

        return if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id)
            logger.info("문서 삭제 완료: id=$id")
            true
        } else {
            logger.warn("삭제할 문서를 찾을 수 없음: id=$id")
            false
        }
    }

    @Transactional
    fun deleteAll() {
        logger.warn("모든 문서 삭제")
        documentRepository.deleteAll()
        logger.info("모든 문서 삭제 완료")
    }

    fun count(): Long {
        return documentRepository.count()
    }

    fun countByDocumentType(documentType: DocumentType): Long {
        val pageable = PageRequest.of(0, 1)
        return documentRepository.findByDocumentType(documentType, pageable).totalElements
    }
}