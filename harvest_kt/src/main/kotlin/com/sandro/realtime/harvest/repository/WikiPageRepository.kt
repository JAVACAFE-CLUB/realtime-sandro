package com.sandro.realtime.harvest.repository

import com.sandro.realtime.harvest.domain.SourceContent
import com.sandro.realtime.harvest.domain.SourceType
import com.sandro.realtime.harvest.domain.WikiPage
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

        val bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SourceContent::class.java)
        val sourceContents = mutableListOf<SourceContent>()

        wikiPages.forEach { wikiPage ->
            val sourceContent = SourceContent.fromWikiPage(wikiPage)
            sourceContents.add(sourceContent)

            // TODO: 중복제거 확인하기
            // 기존과 동일한 조건부 쿼리: revision이 다를 때만 upsert
            val query = Query.query(
                Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                    .and("content.id").`is`(wikiPage.id)
//                    .and("content.revision.id").ne(wikiPage.revision.id)
            )

            val update = Update()
                .set("content", sourceContent.content)
                .set("processedAt", LocalDateTime.now())
                .setOnInsert("type", SourceType.WIKIPEDIA)
                .setOnInsert("createdAt", LocalDateTime.now())

            bulkOps.upsert(query, update)
        }

        return try {
            val result = bulkOps.execute()

            // 실제로 변경된 문서들의 ID 목록
            val upsertedIds = result.upserts.map { it.id }

            if (upsertedIds.isNotEmpty()) {
                // 변경된 문서들만 조회해서 반환
                mongoTemplate.find(
                    Query.query(Criteria.where("_id").`in`(upsertedIds)),
                    SourceContent::class.java
                )
            } else {
                emptyList()
            }

        } catch (e: Exception) {
            logger.error("Failed to bulk upsert wiki pages: count=${wikiPages.size}", e)
            throw e
        }
    }
}