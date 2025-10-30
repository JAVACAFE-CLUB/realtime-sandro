package com.sandro.realtime.harvest.common.service

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import com.sandro.realtime.harvest.common.domain.SourceContent
import com.sandro.realtime.harvest.wiki.event.WikiPagesBatchProcessedEvent
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.LocalDateTime

// TODO: kotest로 변환
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = [KafkaTopic.WIKI_CONTENT_PROCESSED])
@TestPropertySource(properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
class ContentKafkaServiceTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    private lateinit var consumer: Consumer<String, ContentProcessedMessage>

    @BeforeEach
    fun setUp() {
        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker)
        val consumerFactory = DefaultKafkaConsumerFactory(
            consumerProps,
            StringDeserializer(),
            JsonDeserializer(ContentProcessedMessage::class.java).apply {
                addTrustedPackages("*")
            }
        )
        consumer = consumerFactory.createConsumer()
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer)
    }

    @Test
    fun `여러 페이지를 포함한 이벤트 처리 시 모든 메시지가 발송되는지 검증`() {
        // given
        val sourceContents = listOf(
            SourceContent(
                id = "test-id-1",
                type = SourceType.WIKIPEDIA,
                processedAt = LocalDateTime.now(),
                content = mapOf("title" to "Page 1", "text" to "Content 1")
            ),
            SourceContent(
                id = "test-id-2",
                type = SourceType.WIKIPEDIA,
                processedAt = LocalDateTime.now(),
                content = mapOf("title" to "Page 2", "text" to "Content 2")
            )
        )

        val event = WikiPagesBatchProcessedEvent(sourceContents)

        // when
        eventPublisher.publishEvent(event)

        // then
        val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5))

        assertThat(records).hasSize(2)

        val messageIds = records.map { it.value().id }
        assertThat(messageIds).containsExactlyInAnyOrder("test-id-1", "test-id-2")
    }
}