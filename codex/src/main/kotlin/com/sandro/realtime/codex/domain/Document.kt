package com.sandro.realtime.codex.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDateTime

@Document(indexName = "documents")
data class Document(
    @Id
    val id: String? = null,

    @Field(type = FieldType.Keyword)
    val documentType: DocumentType,

    @Field(type = FieldType.Text, analyzer = "standard")
    val title: String? = null,

    @Field(type = FieldType.Text, analyzer = "standard")
    val content: String,

    @Field(type = FieldType.Keyword)
    val source: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_fraction])
    val publishedAt: LocalDateTime? = null,

    @Field(type = FieldType.Keyword)
    val author: String? = null,

    @Field(type = FieldType.Keyword)
    val categories: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val tags: List<String> = emptyList(),

    @Field(type = FieldType.Text)
    val url: String? = null,

    @Field(type = FieldType.Object)
    val metadata: Map<String, Any> = emptyMap(),

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_fraction])
    val indexedAt: LocalDateTime = LocalDateTime.now()
)

enum class DocumentType {
    WIKI,     // 위키페이지
    NEWS,     // 뉴스 HTML
    TWEET     // X API 응답
}