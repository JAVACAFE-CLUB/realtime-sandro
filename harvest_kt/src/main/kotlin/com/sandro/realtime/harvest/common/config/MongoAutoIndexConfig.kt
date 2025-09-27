package com.sandro.realtime.harvest.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.IndexResolver
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty

/**
 * IndexResolver를 사용한 자동 인덱스 생성
 *
 * @Document가 있는 모든 Entity의 인덱스를 자동으로 감지하고 생성합니다.
 * @CompoundIndex, @Indexed 등의 어노테이션을 모두 처리합니다.
 */
@Configuration
class MongoAutoIndexConfig(
    private val mongoTemplate: MongoTemplate
) {

    @EventListener(ContextRefreshedEvent::class)
    fun initIndicesAfterStartup() {
        val mappingContext: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty> =
            mongoTemplate.converter.mappingContext

        val resolver: IndexResolver = MongoPersistentEntityIndexResolver(mappingContext)

        // @Document가 있는 모든 엔티티를 찾아서 인덱스 생성
        mappingContext.persistentEntities
            .stream()
            .filter { it.isAnnotationPresent(Document::class.java) }
            .forEach { entity ->
                val indexOps = mongoTemplate.indexOps(entity.type)
                resolver.resolveIndexFor(entity.type).forEach { indexOps.createIndex(it) }
            }
    }
}