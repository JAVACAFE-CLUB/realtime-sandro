package com.sandro.realtime.harvest.wiki.repository

import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import com.sandro.realtime.harvest.wiki.domain.WikiPage
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * WikiPage MongoDB 저장소
 * 순수한 데이터 액세스 레이어 - CRUD 연산만 담당
 */
@Repository
class WikiPageRepository(
    private val mongoTemplate: MongoTemplate
) {
    private val logger = LoggerFactory.getLogger(WikiPageRepository::class.java)

    /**
     * 배치 Revision 조건부 Upsert
     * MongoDB Bulk Operations를 통해 한 번의 네트워크 호출로 처리
     *
     * @param wikiPages upsert할 WikiPage 리스트
     * @return 생성/업데이트된 SourceContent 리스트
     */
    fun bulkUpsertWithRevisionCheck(wikiPages: List<WikiPage>): List<SourceContent> {
        if (wikiPages.isEmpty()) return emptyList()

        val pagesToUpdate = filterPagesToUpdate(wikiPages)

        if (pagesToUpdate.isEmpty()) return emptyList()

        val bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SourceContent::class.java)
        val sourceContents = mutableListOf<SourceContent>()

        pagesToUpdate.forEach { wikiPage ->
            val sourceContent = fromWikiPage(wikiPage)
            sourceContents.add(sourceContent)

            val upsertQuery = Query.query(
                Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                    .and("content.id").`is`(wikiPage.id)
            )

            val update = Update()
                .set("content", sourceContent.content)
                .set("processedAt", LocalDateTime.now())
                .setOnInsert("type", SourceType.WIKIPEDIA)
                .setOnInsert("createdAt", LocalDateTime.now())

            bulkOps.upsert(upsertQuery, update)
        }

        return try {
            bulkOps.execute()
            pagesToUpdate.map { fromWikiPage(it) }
        } catch (e: Exception) {
            logger.error("Failed to bulk upsert wiki pages: count=${wikiPages.size}", e)
            throw e
        }
    }

    private fun filterPagesToUpdate(wikiPages: List<WikiPage>): List<WikiPage> {
        // 먼저 기존 문서들의 revision 정보만 조회 (Projection 사용)
        val wikiPageIds = wikiPages.map { it.id }
        val query = Query.query(
            Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                .and("content.id").`in`(wikiPageIds)
        )
        // content.id와 content.revision.id만 조회하여 성능 최적화
        query.fields()
            .include("type")  // SourceContent 객체 생성에 필요
            .include("content.id")
            .include("content.revision.id")

        val existingDocs = mongoTemplate.find(query, SourceContent::class.java)

        // 기존 revision id를 페이지 id와 매핑 (Map에서 직접 추출)
        val existingRevisions = existingDocs.associate { doc ->
            val id = (doc.content["id"] as? Number)?.toLong()
                ?: throw IllegalStateException("Missing or invalid content.id in document")

            val revisionId = ((doc.content["revision"] as? Map<*, *>)?.get("id") as? Number)?.toLong()
                ?: throw IllegalStateException("Missing or invalid content.revision.id for page id: $id")

            id to revisionId
        }

        // revision이 변경된 페이지만 필터링
        return wikiPages.filter { wikiPage -> existingRevisions[wikiPage.id] != wikiPage.revision.id }
    }

    private fun fromWikiPage(wikiPage: WikiPage): SourceContent {
        return SourceContent.from(wikiPage)
    }
}