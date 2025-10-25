package com.sandro.realtime.harvest.news.service

import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.repository.SourceContentRepository
import com.sandro.realtime.harvest.common.util.SourceContentMapper
import com.sandro.realtime.harvest.news.domain.NewsArticle
import org.springframework.stereotype.Service

@Service
class ArticleService(
    private val sourceContentRepository: SourceContentRepository,
) {

    fun upsert(article: NewsArticle): SourceContent? {
        val optionalContent = sourceContentRepository.findByTypeAndContentArticleId(SourceType.NEWS, article.articleId)

        return if (optionalContent.isPresent) {
            updateIfNewer(article, optionalContent.get())
        } else {
            createNew(article)
        }
    }

    private fun updateIfNewer(article: NewsArticle, existing: SourceContent): SourceContent? {
        if (!article.hasLastModifiedAt()) return null

        val existingLastModified = existing.getLastModifiedAt()

        return when {
            existingLastModified == null || article.hasUpdated(existingLastModified) -> {
                existing.update(SourceContentMapper.from(article))
                sourceContentRepository.save(existing)
            }

            else -> null
        }
    }

    private fun createNew(article: NewsArticle): SourceContent {
        val sourceContent = SourceContentMapper.from(article)
        return sourceContentRepository.save(sourceContent)
    }

}