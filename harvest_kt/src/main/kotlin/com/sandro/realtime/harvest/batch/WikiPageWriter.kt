package com.sandro.realtime.harvest.batch

import com.sandro.realtime.harvest.domain.WikiPage
import com.sandro.realtime.harvest.service.WikiPageStorageService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class WikiPageWriter(
    private val wikiPageStorageService: WikiPageStorageService
) : ItemWriter<WikiPage> {

    private val logger = LoggerFactory.getLogger(WikiPageWriter::class.java)

    override fun write(chunk: Chunk<out WikiPage>) {
        val items = chunk.items.toList()

        try {
            val savedPages = wikiPageStorageService.storeWikiPages(items)

            // 저장된 페이지들 로깅
            savedPages.forEach { harvestedPage ->
                val content = harvestedPage.content
                logger.info("Stored WikiPage:")
                logger.info("  MongoDB ID: ${harvestedPage.id}")
                logger.info("  Title: ${content["title"]}")
                logger.info("  Page ID: ${content["id"]}")
                logger.info("  Source Type: ${harvestedPage.type}")
                logger.info("  Revision ID: ${(content["revision"] as? Map<*, *>)?.get("id")}")
                logger.debug("=" + "=".repeat(79))
            }

            val duplicateCount = items.size - savedPages.size
            logger.info("Chunk processed - New: ${savedPages.size}, Duplicates: $duplicateCount, Total: ${items.size}")

        } catch (e: Exception) {
            logger.error("Failed to process chunk of ${items.size} pages", e)
            // Spring Batch가 chunk 실패를 처리하도록 예외를 다시 던짐
            throw e
        }
    }
}