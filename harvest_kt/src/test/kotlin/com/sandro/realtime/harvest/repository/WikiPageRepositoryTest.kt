package com.sandro.realtime.harvest.repository

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import com.sandro.realtime.harvest.domain.SourceContent
import com.sandro.realtime.harvest.domain.SourceType
import com.sandro.realtime.harvest.domain.WikiPage
import com.sandro.realtime.harvest.domain.WikiRevision
import io.kotest.matchers.collections.shouldNotBeEmpty
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

@ActiveProfiles("test")
@DataMongoTest
class WikiPageRepositoryTest {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var sourceContentRepository: SourceContentRepository

    lateinit var wikiPageRepository: WikiPageRepository

    private val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .build()

    @BeforeEach
    fun setUp() {
        wikiPageRepository = WikiPageRepository(mongoTemplate)
        // 테스트 전 컬렉션 정리
        mongoTemplate.dropCollection(SourceContent::class.java)
    }

    @Test
    fun `새로운 WikiPage를 정상적으로 저장한다`() {
        // given
        val wikiRevision = fixtureMonkey.giveMeKotlinBuilder<WikiRevision>()
            .setExp(WikiRevision::id, 100L)
            .setExp(WikiRevision::text, "테스트 내용")
            .sample()

        val wikiPage = fixtureMonkey.giveMeKotlinBuilder<WikiPage>()
            .setExp(WikiPage::id, 1L)
            .setExp(WikiPage::title, "테스트 페이지")
            .setExp(WikiPage::revision, wikiRevision)
            .sample()

        // when
        val result = wikiPageRepository.bulkUpsertWithRevisionCheck(listOf(wikiPage))

        // then
        result.shouldNotBeEmpty()
        result.size shouldBe 1

        // MongoDB에 실제 저장 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                    .and("content.id").`is`(1L)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1
        (savedDocs[0].content["id"] as Number).toLong() shouldBe 1L
    }

    @Test
    fun `revision이 다른 경우 기존 문서를 업데이트한다`() {
        // given
        // 기존 문서 저장 (revision id: 100)
        val sourceContent = SourceContent(
            type = SourceType.WIKIPEDIA,
            processedAt = LocalDateTime.now(),
            content = mapOf(
                "id" to 1L,
                "title" to "기존 페이지",
                "revision" to mapOf(
                    "id" to 100L,
                    "text" to "기존 내용"
                )
            )
        )

        sourceContentRepository.save(sourceContent)

        // 새로운 revision으로 업데이트할 WikiPage (revision id: 200)
        val newRevision = fixtureMonkey.giveMeKotlinBuilder<WikiRevision>()
            .setExp(WikiRevision::id, 200L)
            .setExp(WikiRevision::text, "업데이트된 내용")
            .sample()

        val updatedWikiPage = fixtureMonkey.giveMeKotlinBuilder<WikiPage>()
            .setExp(WikiPage::id, 1L)  // 같은 ID
            .setExp(WikiPage::title, "업데이트된 페이지")
            .setExp(WikiPage::revision, newRevision)  // 다른 revision
            .sample()

        // when
        val result = wikiPageRepository.bulkUpsertWithRevisionCheck(listOf(updatedWikiPage))

        // then
        result.shouldNotBeEmpty()
        result.size shouldBe 1

        // MongoDB에 업데이트된 문서 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                    .and("content.id").`is`(1L)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1  // 여전히 1개의 문서만 존재
        val savedContent = savedDocs[0].content
        (savedContent["id"] as Number).toLong() shouldBe 1L
        ((savedContent["revision"] as Map<*, *>)["id"] as Number).toLong() shouldBe 200L  // revision이 업데이트됨
        (savedContent["title"] as String) shouldBe "업데이트된 페이지"
    }

    @Test
    fun `revision이 같은 경우 업데이트하지 않는다`() {
        // given
        // 기존 문서 저장 (revision id: 100)
        val revision = fixtureMonkey.giveMeKotlinBuilder<WikiRevision>()
            .setExp(WikiRevision::id, 100L)
            .setExp(WikiRevision::text, "기존 내용")
            .sample()

        val wikiPage = fixtureMonkey.giveMeKotlinBuilder<WikiPage>()
            .setExp(WikiPage::id, 1L)
            .setExp(WikiPage::title, "기존 페이지")
            .setExp(WikiPage::revision, revision)
            .sample()

        sourceContentRepository.save(SourceContent.fromWikiPage(wikiPage))

        // when
        val result = wikiPageRepository.bulkUpsertWithRevisionCheck(listOf(wikiPage))

        // then
        result.size shouldBe 0  // revision이 같으므로 업데이트되지 않음

        // MongoDB에서 기존 문서가 변경되지 않았는지 확인
        val savedDocs = mongoTemplate.find(
            Query.query(
                Criteria.where("type").`is`(SourceType.WIKIPEDIA)
                    .and("content.id").`is`(1L)
            ),
            SourceContent::class.java
        )
        savedDocs.size shouldBe 1  // 여전히 1개의 문서만 존재
        val savedContent = savedDocs[0].content
        (savedContent["id"] as Number).toLong() shouldBe 1L
        ((savedContent["revision"] as Map<*, *>)["id"] as Number).toLong() shouldBe 100L  // revision은 그대로
        (savedContent["title"] as String) shouldBe "기존 페이지"  // 제목도 변경되지 않음
    }
}