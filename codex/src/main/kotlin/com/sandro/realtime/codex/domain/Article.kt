package com.sandro.realtime.codex.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDateTime

@Document(indexName = "articles")
data class Article(
    @Id
    val id: String? = null,

    @Field(type = FieldType.Text)
    val title: String,

    @Field(type = FieldType.Text)
    val content: String,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_fraction])
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_fraction])
    val updatedAt: LocalDateTime = LocalDateTime.now()
)