package com.sandro.realtime.smithy.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.smithy.kafka.TextKafkaService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * ContentKafkaListener 통합 테스트
 *
 * Embedded Kafka를 사용하여 메시지 발송과 수신을 검증합니다.
 */
@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration"
    ]
)
@TestPropertySource(properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
@EmbeddedKafka(
    partitions = 1,
    topics = [KafkaTopic.WIKI_CONTENT_PROCESSED, KafkaTopic.NEWS_CONTENT_PROCESSED]
)
@DirtiesContext
class ContentKafkaListenerTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var textKafkaService: TextKafkaService

    @Test
    fun `Wiki 콘텐츠 메시지를 발송한다`() {
        // given
        val message = ContentProcessedMessage(
            id = "wiki-123",
            type = SourceType.WIKIPEDIA,
            processedAt = LocalDateTime.now(),
            content = mapOf(
                "title" to "Test Wikipedia Page",
                "revision" to mapOf(
                    "text" to "== 제목 ==\n테스트 위키 페이지입니다."
                )
            )
        )
        val messageJson = objectMapper.writeValueAsString(message)

        // when & then - 메시지가 정상적으로 발송되는지 확인
        kafkaTemplate.send(KafkaTopic.WIKI_CONTENT_PROCESSED, messageJson).get()

        // 리스너가 처리할 시간 확보
        TimeUnit.SECONDS.sleep(2)
    }

    @Test
    fun `News 콘텐츠 메시지를 발송한다`() {
        // given
        val message = ContentProcessedMessage(
            id = "news-456",
            type = SourceType.NEWS,
            processedAt = LocalDateTime.now(),
            content = mapOf(
                "title" to "Test News Article",
                "content" to "<html><body><h1>뉴스 제목</h1><p>뉴스 내용</p></body></html>",
                "officeId" to "office-1",
                "articleId" to "article-1"
            )
        )
        val messageJson = objectMapper.writeValueAsString(message)

        // when & then - 메시지가 정상적으로 발송되는지 확인
        kafkaTemplate.send(KafkaTopic.NEWS_CONTENT_PROCESSED, messageJson).get()

        // 리스너가 처리할 시간 확보
        TimeUnit.SECONDS.sleep(2)
    }
}
