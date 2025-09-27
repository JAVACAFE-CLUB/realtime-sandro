package com.sandro.realtime.harvest.news.service

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.common.domain.SourceType
import com.sandro.realtime.harvest.common.repository.SourceContentRepository
import com.sandro.realtime.harvest.news.domain.NewsArticle
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

// TODO: kotest로 변환
@ActiveProfiles("test")
@DataMongoTest
class ArticleServiceTest {

    @Autowired
    lateinit var sourceContentRepository: SourceContentRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    lateinit var sut: ArticleService

    private val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .build()

    @BeforeEach
    fun setUp() {
        sut = ArticleService(sourceContentRepository)
        // 테스트 전 컬렉션 정리
        mongoTemplate.dropCollection(SourceContent::class.java)
    }

    @Test
    fun `신규 기사인 경우 새로운 SourceContent를 생성해야 한다`() {
        // given
        val articleId = "test-article-1"
        val lastModifiedAt = "2024-01-01T12:00:00"

        val article = fixtureMonkey.giveMeKotlinBuilder<NewsArticle>()
            .setExp(NewsArticle::articleId, articleId)
            .setExp(NewsArticle::lastModifiedAt, lastModifiedAt)
            .sample()

        // when
        val result = sut.upsert(article)

        // then
        result.shouldNotBeNull()
        result!!.type shouldBe SourceType.NEWS
        result.content["articleId"] shouldBe articleId
        result.content["lastModifiedAt"] shouldBe lastModifiedAt

        // MongoDB에 실제 저장 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.NEWS)
                    .and("content.articleId").`is`(articleId)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1
        savedDocs[0].content["articleId"] shouldBe articleId
        savedDocs[0].content["lastModifiedAt"] shouldBe lastModifiedAt
    }

    @Test
    fun `기존 기사가 있고 더 최신인 경우 기존 SourceContent를 업데이트해야 한다`() {
        // given
        val articleId = "test-article-1"

        val article = fixtureMonkey.giveMeKotlinBuilder<NewsArticle>()
            .setExp(NewsArticle::articleId, articleId)
            .setExp(NewsArticle::lastModifiedAt, "2024-01-02T12:00:00")
            .sample()

        // 기존 문서 저장 (2024-01-01)
        val existingContent = SourceContent(
            type = SourceType.NEWS,
            processedAt = LocalDateTime.now(),
            content = mutableMapOf("articleId" to articleId, "lastModifiedAt" to "2024-01-01T12:00:00")
        )
        sourceContentRepository.save(existingContent)

        // when
        val result = sut.upsert(article)

        // then
        result.shouldNotBeNull()
        result!!.type shouldBe SourceType.NEWS
        result.content["articleId"] shouldBe articleId
        result.content["lastModifiedAt"] shouldBe "2024-01-02T12:00:00"

        // MongoDB에 업데이트된 문서 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.NEWS)
                    .and("content.articleId").`is`(articleId)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1  // 여전히 1개의 문서만 존재
        savedDocs[0].content["articleId"] shouldBe articleId
        savedDocs[0].content["lastModifiedAt"] shouldBe "2024-01-02T12:00:00"  // 업데이트됨
    }

    @Test
    fun `기존 기사가 있지만 이전 시간인 경우 null을 반환해야 한다`() {
        // given
        val articleId = "test-article-1"

        val article = fixtureMonkey.giveMeKotlinBuilder<NewsArticle>()
            .setExp(NewsArticle::articleId, articleId)
            .setExp(NewsArticle::lastModifiedAt, "2024-01-01T12:00:00")
            .sample()

        // 기존 문서 저장 (더 최신: 2024-01-02)
        val existingContent = SourceContent(
            type = SourceType.NEWS,
            processedAt = LocalDateTime.now(),
            content = mutableMapOf("articleId" to articleId, "lastModifiedAt" to "2024-01-02T12:00:00")
        )
        sourceContentRepository.save(existingContent)

        // when
        val result = sut.upsert(article)

        // then
        result.shouldBeNull()

        // MongoDB에 기존 문서가 변경되지 않았는지 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.NEWS)
                    .and("content.articleId").`is`(articleId)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1
        savedDocs[0].content["articleId"] shouldBe articleId
        savedDocs[0].content["lastModifiedAt"] shouldBe "2024-01-02T12:00:00"  // 기존 시간 그대로
    }

    @Test
    fun `기존 기사가 있고 신규 기사는 lastModifiedAt이 없는 경우 null을 반환해야 한다`() {
        // given
        val articleId = "test-article-1"

        val article = fixtureMonkey.giveMeKotlinBuilder<NewsArticle>()
            .setExp(NewsArticle::articleId, articleId)
            .setExp(NewsArticle::lastModifiedAt, null)
            .sample()

        // 기존 문서 저장
        val existingContent = SourceContent(
            type = SourceType.NEWS,
            processedAt = LocalDateTime.now(),
            content = mapOf("articleId" to articleId, "lastModifiedAt" to "2024-01-01T12:00:00")
        )
        sourceContentRepository.save(existingContent)

        // when
        val result = sut.upsert(article)

        // then
        result.shouldBeNull()

        // MongoDB에 기존 문서가 변경되지 않았는지 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.NEWS)
                    .and("content.articleId").`is`(articleId)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1
        savedDocs[0].content["articleId"] shouldBe articleId
        savedDocs[0].content["lastModifiedAt"] shouldBe "2024-01-01T12:00:00"  // 기존 시간 그대로
    }

    @Test
    fun `기존 데이터에 lastModifiedAt이 없고 신규 기사는 있는 경우 기존 SourceContent를 업데이트해야 한다`() {
        // given
        val articleId = "test-article-1"

        val article = fixtureMonkey.giveMeKotlinBuilder<NewsArticle>()
            .setExp(NewsArticle::articleId, articleId)
            .setExp(NewsArticle::lastModifiedAt, "2024-01-01T12:00:00")
            .sample()

        // 기존 문서 저장 (lastModifiedAt 없음)
        val existingContent = SourceContent(
            type = SourceType.NEWS,
            processedAt = LocalDateTime.now(),
            content = mutableMapOf("articleId" to articleId)  // lastModifiedAt 없음
        )
        sourceContentRepository.save(existingContent)

        // when
        val result = sut.upsert(article)

        // then
        result.shouldNotBeNull()
        result!!.type shouldBe SourceType.NEWS
        result.content["articleId"] shouldBe articleId
        result.content["lastModifiedAt"] shouldBe "2024-01-01T12:00:00"

        // MongoDB에 업데이트된 문서 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.NEWS)
                    .and("content.articleId").`is`(articleId)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1  // 여전히 1개의 문서만 존재
        savedDocs[0].content["articleId"] shouldBe articleId
        savedDocs[0].content["lastModifiedAt"] shouldBe "2024-01-01T12:00:00"  // lastModifiedAt 추가됨
    }
}