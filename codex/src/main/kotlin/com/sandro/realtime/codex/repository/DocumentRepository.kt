package com.sandro.realtime.codex.repository

import com.sandro.realtime.codex.domain.Document
import com.sandro.realtime.codex.domain.DocumentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : ElasticsearchRepository<Document, String> {

    fun findByTitle(title: String): List<Document>

    fun findByTitleContaining(title: String, pageable: Pageable): Page<Document>

    fun findByContentContaining(content: String, pageable: Pageable): Page<Document>

    fun findByCategoriesContaining(category: String, pageable: Pageable): Page<Document>

    fun findByDocumentType(documentType: DocumentType, pageable: Pageable): Page<Document>

    fun findByDocumentTypeAndTitleContaining(
        documentType: DocumentType,
        title: String,
        pageable: Pageable
    ): Page<Document>

    fun findByDocumentTypeAndContentContaining(
        documentType: DocumentType,
        content: String,
        pageable: Pageable
    ): Page<Document>

    fun findByDocumentTypeAndCategoriesContaining(
        documentType: DocumentType,
        category: String,
        pageable: Pageable
    ): Page<Document>

    fun findBySource(source: String, pageable: Pageable): Page<Document>

    fun findByAuthor(author: String, pageable: Pageable): Page<Document>

    fun findByTagsContaining(tag: String, pageable: Pageable): Page<Document>

    @Query(
        """
        {
            "bool": {
                "should": [
                    { "match": { "title": "?0" } },
                    { "match": { "content": "?0" } }
                ],
                "minimum_should_match": 1
            }
        }
    """
    )
    fun searchByTitleOrContent(query: String, pageable: Pageable): Page<Document>

    @Query(
        """
        {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "?0",
                            "fields": ["title^2", "content"],
                            "type": "best_fields"
                        }
                    }
                ],
                "filter": [
                    { "term": { "documentType": "?1" } }
                ]
            }
        }
    """
    )
    fun searchByQueryAndDocumentType(
        query: String,
        documentType: DocumentType,
        pageable: Pageable
    ): Page<Document>

    @Query(
        """
        {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "?0",
                            "fields": ["title^2", "content"],
                            "type": "best_fields"
                        }
                    }
                ],
                "filter": [
                    { "term": { "categories": "?1" } }
                ]
            }
        }
    """
    )
    fun searchByQueryAndCategory(query: String, category: String, pageable: Pageable): Page<Document>

    @Query(
        """
        {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "?0",
                            "fields": ["title^2", "content"],
                            "type": "best_fields"
                        }
                    }
                ],
                "filter": [
                    { "term": { "documentType": "?1" } },
                    { "term": { "categories": "?2" } }
                ]
            }
        }
    """
    )
    fun searchByQueryAndDocumentTypeAndCategory(
        query: String,
        documentType: DocumentType,
        category: String,
        pageable: Pageable
    ): Page<Document>
}