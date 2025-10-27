package com.sandro.realtime.harvest.loadtest

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 부하 테스트 REST API 컨트롤러
 *
 * 개발/테스트 환경에서만 활성화되며, Kafka 컨슈머 성능 테스트를 위한 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/loadtest")
@Profile("dev", "local")
class LoadTestController(
    private val loadTestService: LoadTestService
) {

    /**
     * 부하 테스트 시작
     *
     * POST /api/loadtest/start?throughput=10000&duration=60
     *
     * @param throughput 초당 목표 메시지 수 (기본값: 10000)
     * @param duration 테스트 지속 시간(초) (기본값: 60)
     * @return 시작 결과
     */
    @PostMapping("/start")
    fun startLoadTest(
        @RequestParam(defaultValue = "10000") throughput: Int,
        @RequestParam(defaultValue = "60") duration: Int
    ): ResponseEntity<Map<String, Any>> {
        // 유효성 검사
        if (throughput <= 0 || throughput > 100000) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "throughput은 1~100000 사이여야 합니다"
                )
            )
        }

        if (duration <= 0 || duration > 3600) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "duration은 1~3600 사이여야 합니다"
                )
            )
        }

        val started = loadTestService.startLoadTest(throughput, duration)

        return if (started) {
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "부하 테스트가 시작되었습니다",
                    "targetThroughput" to throughput,
                    "targetDuration" to duration,
                    "totalMessages" to (throughput * duration)
                )
            )
        } else {
            ResponseEntity.status(409).body(
                mapOf(
                    "success" to false,
                    "message" to "이미 실행 중인 부하 테스트가 있습니다"
                )
            )
        }
    }

    /**
     * 부하 테스트 상태 조회
     *
     * GET /api/loadtest/status
     *
     * @return 현재 상태
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<LoadTestStatus> {
        val status = loadTestService.getStatus()
        return ResponseEntity.ok(status)
    }

    /**
     * 부하 테스트 중단
     *
     * POST /api/loadtest/stop
     *
     * @return 중단 결과
     */
    @PostMapping("/stop")
    fun stopLoadTest(): ResponseEntity<Map<String, Any>> {
        loadTestService.stopLoadTest()

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "부하 테스트 중단 요청이 전송되었습니다"
            )
        )
    }

    /**
     * 간편 테스트 엔드포인트 모음
     */
    @GetMapping("/quick")
    fun quickTests(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "1000/sec" to "/api/loadtest/start?throughput=1000&duration=30",
                "5000/sec" to "/api/loadtest/start?throughput=5000&duration=60",
                "10000/sec" to "/api/loadtest/start?throughput=10000&duration=60",
                "status" to "/api/loadtest/status",
                "stop" to "/api/loadtest/stop (POST)"
            )
        )
    }
}
