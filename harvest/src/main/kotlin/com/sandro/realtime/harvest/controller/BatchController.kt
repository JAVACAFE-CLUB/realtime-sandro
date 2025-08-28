package com.sandro.realtime.harvest.controller

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/batch")
class BatchController(
    private val jobLauncher: JobLauncher,
    private val fileProcessingJob: Job
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/start")
    fun startBatch(): String {
        return try {
            val jobParameters = JobParametersBuilder()
                .addString("startTime", LocalDateTime.now().toString())
                .toJobParameters()

            val jobExecution = jobLauncher.run(fileProcessingJob, jobParameters)

            logger.info("배치 작업 시작: ${jobExecution.id}")
            "배치 작업이 시작되었습니다. Job ID: ${jobExecution.id}"
        } catch (e: Exception) {
            logger.error("배치 작업 시작 실패", e)
            "배치 작업 시작 실패: ${e.message}"
        }
    }
}