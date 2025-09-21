package com.sandro.realtime.harvest.service

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.harvest.domain.SourceContent
import com.sandro.realtime.harvest.domain.SourceType
import com.sandro.realtime.harvest.event.WikiPagesBatchProcessedEvent
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.function.Consumer

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = [KafkaTopic.WIKI_CONTENT_PROCESSED])
@TestPropertySource(properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
class ContentKafkaServiceTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    private lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    private lateinit var producer: Producer<String, ContentProcessedMessage>

    @BeforeEach
    fun setUp() {
        producer = DefaultKafkaProducerFactory<String, ContentProcessedMessage>(
            KafkaTestUtils.producerProps(embeddedKafkaBroker),
            StringSerializer(),
            JsonSerializer()
        ).createProducer()

        kafkaListenerEndpointRegistry.getAllListenerContainers()
            .forEach(Consumer { messageListenerContainer: MessageListenerContainer? ->
                if (messageListenerContainer!!.isAutoStartup) ContainerTestUtils.waitForAssignment(
                    messageListenerContainer,
                    embeddedKafkaBroker.partitionsPerTopic
                )
            })
    }

    @Test
    fun `WikiPagesBatchProcessedEvent 처리 테스트`() {
        // given
        val sourceContent = SourceContent(
            id = "test-id",
            type = SourceType.WIKIPEDIA,
            processedAt = LocalDateTime.now(),
            content = mapOf("title" to "Test Page", "text" to "Test Content")
        )

        val event = WikiPagesBatchProcessedEvent(listOf(sourceContent))

        // when & then
        eventPublisher.publishEvent(event)
    }
}