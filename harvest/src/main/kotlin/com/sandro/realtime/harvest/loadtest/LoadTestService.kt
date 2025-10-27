package com.sandro.realtime.harvest.loadtest

import com.sandro.realtime.common.KafkaTopic
import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 부하 테스트용 메시지 발행 서비스
 *
 * 개발/테스트 환경에서만 활성화되며, Kafka 컨슈머 성능 테스트를 위한 대량 메시지를 발행합니다.
 */
@Service
@Profile("dev", "local")
class LoadTestService(
    private val kafkaTemplate: KafkaTemplate<String, ContentProcessedMessage>
) {

    private val logger = LoggerFactory.getLogger(LoadTestService::class.java)

    private val isRunning = AtomicBoolean(false)
    private val shouldStop = AtomicBoolean(false)
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)
    private val startTime = AtomicLong(0)

    private var targetThroughput = 0
    private var targetDuration = 0

    private val executor = Executors.newFixedThreadPool(10)

    /**
     * 부하 테스트 시작
     *
     * @param throughputPerSecond 초당 목표 메시지 수 (예: 10000)
     * @param durationSeconds 테스트 지속 시간 (초)
     * @return 시작 성공 여부
     */
    fun startLoadTest(throughputPerSecond: Int, durationSeconds: Int): Boolean {
        if (isRunning.get()) {
            logger.warn("부하 테스트가 이미 실행 중입니다")
            return false
        }

        // 상태 초기화
        isRunning.set(true)
        shouldStop.set(false)
        successCount.set(0)
        failureCount.set(0)
        startTime.set(System.currentTimeMillis())
        targetThroughput = throughputPerSecond
        targetDuration = durationSeconds

        logger.info("===== Kafka 부하 테스트 시작 =====")
        logger.info("목표 처리량: $throughputPerSecond msg/sec")
        logger.info("테스트 시간: $durationSeconds 초")

        // 별도 스레드에서 실행
        Thread {
            try {
                publishMessagesInternal(throughputPerSecond, durationSeconds)
            } catch (e: Exception) {
                logger.error("부하 테스트 중 예외 발생", e)
            } finally {
                isRunning.set(false)
                logger.info("===== 부하 테스트 완료 =====")
                logger.info("성공: ${successCount.get()} / 실패: ${failureCount.get()}")
            }
        }.start()

        return true
    }

    /**
     * 부하 테스트 중단
     */
    fun stopLoadTest() {
        if (!isRunning.get()) {
            logger.warn("실행 중인 부하 테스트가 없습니다")
            return
        }

        logger.info("부하 테스트 중단 요청")
        shouldStop.set(true)
    }

    /**
     * 현재 상태 조회
     */
    fun getStatus(): LoadTestStatus {
        val elapsed = if (startTime.get() > 0) {
            Duration.between(
                LocalDateTime.now().minusSeconds((System.currentTimeMillis() - startTime.get()) / 1000),
                LocalDateTime.now()
            ).seconds
        } else {
            0L
        }

        val actualThroughput = if (elapsed > 0) {
            successCount.get().toDouble() / elapsed
        } else {
            0.0
        }

        return if (isRunning.get()) {
            LoadTestStatus(
                isRunning = true,
                startTime = LocalDateTime.now().minusSeconds(elapsed),
                targetThroughput = targetThroughput,
                targetDuration = targetDuration,
                totalMessages = targetThroughput * targetDuration,
                successCount = successCount.get(),
                failureCount = failureCount.get(),
                elapsedSeconds = elapsed,
                actualThroughput = actualThroughput
            )
        } else {
            LoadTestStatus.idle()
        }
    }

    /**
     * 실제 메시지 발행 로직
     */
    private fun publishMessagesInternal(throughputPerSecond: Int, durationSeconds: Int) {
        val totalMessages = throughputPerSecond * durationSeconds
        val batchSize = 100  // 배치당 메시지 수
        val batchCount = totalMessages / batchSize
        val delayBetweenBatchesMs = (1000.0 / (throughputPerSecond / batchSize)).toLong()

        logger.info("총 메시지 수: $totalMessages")
        logger.info("배치 크기: $batchSize")
        logger.info("배치 개수: $batchCount")
        logger.info("배치 간 지연: ${delayBetweenBatchesMs}ms")

        for (batchIndex in 0 until batchCount) {
            if (shouldStop.get()) {
                logger.info("부하 테스트 중단됨 (진행률: ${batchIndex * 100 / batchCount}%)")
                break
            }

            // 배치 메시지 생성 및 전송
            val startIdx = batchIndex * batchSize
            for (i in 0 until batchSize) {
                val message = createWikiContentMessage(startIdx + i)

                executor.submit {
                    kafkaTemplate.send(KafkaTopic.WIKI_CONTENT_PROCESSED, message)
                        .whenComplete { _, ex ->
                            if (ex == null) {
                                successCount.incrementAndGet()
                            } else {
                                failureCount.incrementAndGet()
                                logger.error("메시지 전송 실패: ${ex.message}")
                            }
                        }
                }
            }

            // 처리량 조절을 위한 대기
            Thread.sleep(delayBetweenBatchesMs)

            // 진행률 로그 (10%마다)
            if ((batchIndex + 1) % (batchCount / 10) == 0) {
                val progress = (batchIndex + 1) * 100 / batchCount
                logger.info("진행률: $progress% (${successCount.get()}/${totalMessages})")
            }
        }

        // 모든 메시지 전송 완료 대기
        Thread.sleep(3000)
    }

    /**
     * 테스트용 Wiki 콘텐츠 메시지 생성
     */
    private fun createWikiContentMessage(index: Int): ContentProcessedMessage {
        return ContentProcessedMessage(
            id = "wiki-loadtest-$index",
            type = SourceType.WIKIPEDIA,
            processedAt = LocalDateTime.now(),
            content = mapOf(
                "revision" to mapOf(
                    "text" to generateWikiText(index),
                    "timestamp" to LocalDateTime.now().toString(),
                    "comment" to "Load test revision $index"
                ),
                "title" to "Load Test Article $index",
                "namespace" to 0
            )
        )
    }

    /**
     * 테스트용 Wiki 텍스트 생성 (다양한 크기)
     */
    private fun generateWikiText(index: Int): String {
        val sizes = listOf(1000, 3000, 5000, 8000, 10000)  // 1KB ~ 10KB
        val size = sizes[index % sizes.size]

        return buildString {
            append("== Load Test Article $index ==\n\n")
            append("This is a load test article generated for Kafka consumer performance testing.\n\n")
            append("=== Content Section ===\n\n")

            // 다양한 크기의 텍스트 생성
            repeat(size / 100) {
                append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
                append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
            }

            append("\n\n=== References ===\n")
            append("* Reference 1\n")
            append("* Reference 2\n")
            append("* Reference 3\n")
        }
    }
}
