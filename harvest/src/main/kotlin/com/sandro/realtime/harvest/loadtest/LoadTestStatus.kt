package com.sandro.realtime.harvest.loadtest

import java.time.LocalDateTime

/**
 * 부하 테스트 상태를 나타내는 데이터 클래스
 */
data class LoadTestStatus(
    val isRunning: Boolean,
    val startTime: LocalDateTime?,
    val targetThroughput: Int,
    val targetDuration: Int,
    val totalMessages: Int,
    val successCount: Int,
    val failureCount: Int,
    val elapsedSeconds: Long,
    val actualThroughput: Double
) {
    companion object {
        fun idle(): LoadTestStatus {
            return LoadTestStatus(
                isRunning = false,
                startTime = null,
                targetThroughput = 0,
                targetDuration = 0,
                totalMessages = 0,
                successCount = 0,
                failureCount = 0,
                elapsedSeconds = 0,
                actualThroughput = 0.0
            )
        }
    }
}
