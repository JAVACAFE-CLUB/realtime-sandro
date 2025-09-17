package com.sandro.realtime.harvest.batch

import com.sandro.realtime.harvest.domain.WikiPage
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class WikiPageWriter : ItemWriter<WikiPage> {

    private val logger = LoggerFactory.getLogger(WikiPageWriter::class.java)

    override fun write(chunk: Chunk<out WikiPage>) {
        val items = chunk.items
        items.forEach { page ->
            logger.info("Processing WikiPage:")
            logger.info("  Title: ${page.title}")
            logger.info("  ID: ${page.id}")
            logger.info("  Namespace: ${page.namespace}")
            page.revision?.let { revision ->
                logger.info("  Revision ID: ${revision.id}")
                logger.info("  Timestamp: ${revision.timestamp}")
                revision.contributor?.let { contributor ->
                    logger.info("  Contributor: ${contributor.username ?: contributor.ip}")
                }
                revision.text?.let { text ->
                    // 본문 내용이 너무 길어서 처음 200자만 출력
                    val preview = text.take(200) + "..."
                    logger.info("  Text preview: $preview")
                }
            }
            logger.info("=" + "=".repeat(79))
        }
        logger.info("Processed ${items.size} WikiPage(s)")
    }
}