package com.sandro.realtime.harvest.common.config

import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria

/**
 * MongoDB 인덱스 설정
 * Partial Index를 통해 타입별로 효율적인 인덱싱 수행
 * test 프로파일이 아닌 경우에만 실행
 */
@Configuration
class MongoIndexConfig(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 애플리케이션 시작 시 Partial Index 생성
     * 각 소스 타입별로 고유한 중복 체크 인덱스를 생성
     */
    @EventListener(ApplicationReadyEvent::class)
    fun createIndexes() {
        // Wikipedia용 Partial Index: type + pageId 조합으로 중복 체크
        // revision이 다르면 기존 문서를 업데이트하는 방식
        mongoTemplate.indexOps(SourceContent::class.java)
            .createIndex(
                Index()
                    .on("type", Sort.Direction.ASC)
                    .on("content.id", Sort.Direction.ASC)
                    .unique()
                    .named("wiki_page_unique_idx")
                    .partial(PartialIndexFilter.of(Criteria.where("type").`is`(SourceType.WIKIPEDIA)))
            )

        // 조회 성능을 위한 추가 인덱스: revision ID 포함
        mongoTemplate.indexOps(SourceContent::class.java)
            .createIndex(
                Index()
                    .on("type", Sort.Direction.ASC)
                    .on("content.id", Sort.Direction.ASC)
                    .on("content.revision.id", Sort.Direction.ASC)
                    .named("wiki_revision_query_idx")
                    .partial(PartialIndexFilter.of(Criteria.where("type").`is`(SourceType.WIKIPEDIA)))
            )
    }
}