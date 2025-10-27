package com.sandro.realtime.harvest.loadtest

import com.sandro.realtime.common.domain.SourceType
import com.sandro.realtime.common.message.ContentProcessedMessage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.springframework.kafka.core.KafkaTemplate
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

class LoadTestServiceTest : DescribeSpec({

    val kafkaTemplate = mockk<KafkaTemplate<String, ContentProcessedMessage>>(relaxed = true)
    lateinit var loadTestService: LoadTestService

    beforeEach {
        loadTestService = LoadTestService(kafkaTemplate)
    }

    describe("LoadTestService") {

        context("createWikiContentMessage") {
            it("올바른 ContentProcessedMessage를 생성해야 한다") {
                // given
                val index = 42

                // when
                val message = callPrivateMethod<ContentProcessedMessage>(
                    loadTestService,
                    "createWikiContentMessage",
                    index
                )

                // then
                message.id shouldBe "wiki-loadtest-42"
                message.type shouldBe SourceType.WIKIPEDIA
                message.processedAt shouldNotBe null
                message.content shouldNotBe null

                val revision = message.content["revision"] as? Map<*, *>
                revision shouldNotBe null
                revision!!["text"] shouldNotBe null
                revision["timestamp"] shouldNotBe null
                revision["comment"] shouldBe "Load test revision 42"

                message.content["title"] shouldBe "Load Test Article 42"
                message.content["namespace"] shouldBe 0
            }

            it("각 인덱스마다 고유한 ID를 생성해야 한다") {
                // when
                val message1 = callPrivateMethod<ContentProcessedMessage>(
                    loadTestService,
                    "createWikiContentMessage",
                    1
                )
                val message2 = callPrivateMethod<ContentProcessedMessage>(
                    loadTestService,
                    "createWikiContentMessage",
                    2
                )

                // then
                message1.id shouldBe "wiki-loadtest-1"
                message2.id shouldBe "wiki-loadtest-2"
                message1.id shouldNotBe message2.id
            }
        }

        context("generateWikiText") {
            it("다양한 크기의 Wiki 텍스트를 생성해야 한다") {
                // given
                val indices = listOf(0, 1, 2, 3, 4)

                // when & then
                val texts = indices.map { index ->
                    callPrivateMethod<String>(
                        loadTestService,
                        "generateWikiText",
                        index
                    )
                }

                // 각 텍스트가 비어있지 않고, 순서대로 크기가 커지는지 확인
                texts.forEach { text ->
                    text.length shouldNotBe 0
                }

                // 크기가 증가하는 순서인지 확인 (index 0 < 1 < 2 < 3 < 4)
                for (i in 0 until texts.size - 1) {
                    (texts[i].length < texts[i + 1].length) shouldBe true
                }
            }

            it("생성된 Wiki 텍스트에 필수 요소가 포함되어야 한다") {
                // when
                val text = callPrivateMethod<String>(
                    loadTestService,
                    "generateWikiText",
                    0
                )

                // then
                text shouldContain "== Load Test Article 0 =="
                text shouldContain "=== Content Section ==="
                text shouldContain "=== References ==="
                text shouldContain "Lorem ipsum"
            }
        }

        context("getStatus") {
            it("초기 상태는 idle이어야 한다") {
                // when
                val status = loadTestService.getStatus()

                // then
                status.isRunning shouldBe false
                status.startTime shouldBe null
                status.targetThroughput shouldBe 0
                status.targetDuration shouldBe 0
                status.totalMessages shouldBe 0
                status.successCount shouldBe 0
                status.failureCount shouldBe 0
                status.elapsedSeconds shouldBe 0
                status.actualThroughput shouldBe 0.0
            }
        }

        context("startLoadTest") {
            it("부하 테스트를 성공적으로 시작해야 한다") {
                // when
                val result = loadTestService.startLoadTest(
                    throughputPerSecond = 100,
                    durationSeconds = 1
                )

                // then
                result shouldBe true

                // 상태가 실행 중으로 변경되었는지 확인
                Thread.sleep(100) // 비동기 스레드 시작 대기
                val status = loadTestService.getStatus()
                status.isRunning shouldBe true
                status.targetThroughput shouldBe 100
                status.targetDuration shouldBe 1
            }

            it("이미 실행 중인 경우 false를 반환해야 한다") {
                // given
                loadTestService.startLoadTest(100, 1)
                Thread.sleep(100)

                // when
                val result = loadTestService.startLoadTest(200, 2)

                // then
                result shouldBe false
            }
        }

        context("stopLoadTest") {
            it("실행 중인 부하 테스트를 중단할 수 있어야 한다") {
                // given
                loadTestService.startLoadTest(100, 10)
                Thread.sleep(100)

                // when
                loadTestService.stopLoadTest()

                // then
                // 중단 플래그가 설정되었는지는 내부 상태로 확인 불가
                // 하지만 예외 없이 호출 가능해야 함
            }

            it("실행 중이 아닐 때 stopLoadTest를 호출해도 예외가 발생하지 않아야 한다") {
                // when & then
                loadTestService.stopLoadTest()
                // 예외 없이 통과
            }
        }
    }
})

/**
 * Private 메서드를 호출하기 위한 헬퍼 함수
 */
private inline fun <reified T> callPrivateMethod(
    instance: Any,
    methodName: String,
    vararg args: Any?
): T {
    val method = instance::class.declaredFunctions.find { it.name == methodName }
        ?: throw NoSuchMethodException("Method $methodName not found")

    method.isAccessible = true
    return method.call(instance, *args) as T
}
