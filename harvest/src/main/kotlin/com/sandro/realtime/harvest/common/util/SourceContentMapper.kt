package com.sandro.realtime.harvest.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import com.sandro.realtime.harvest.news.domain.NewsArticle
import com.sandro.realtime.harvest.wiki.domain.WikiPage

class SourceContentMapper {
    companion object {
        private val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
        }
        
        private val mapTypeRef = object : TypeReference<Map<String, Any?>>() {}

        /**
         * 범용적인 객체에서 SourceContent로 변환
         */
        fun <T> map(sourceType: SourceType, sourceObject: T): SourceContent {
            return SourceContent(
                type = sourceType,
                content = objectMapper.convertValue(sourceObject, mapTypeRef)
            )
        }

        fun from(wikiPage: WikiPage): SourceContent {
            return map(SourceType.WIKIPEDIA, wikiPage)
        }

        fun from(newsArticle: NewsArticle): SourceContent {
            return map(SourceType.NEWS, newsArticle)
        }
    }
}