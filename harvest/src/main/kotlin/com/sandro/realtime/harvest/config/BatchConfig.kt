package com.sandro.realtime.harvest.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BatchConfig {

    @Bean
    fun fileProcessingJob(
        jobRepository: JobRepository,
        fileProcessingStep: Step
    ): Job {
        return JobBuilder("fileProcessingJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(fileProcessingStep)
            .build()
    }

    @Bean
    fun fileProcessingStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        wikiFileReader: ItemReader<String>,
        simpleProcessor: ItemProcessor<String, String>,
        simpleWriter: ItemWriter<String>
    ): Step {
        return StepBuilder("fileProcessingStep", jobRepository)
            .chunk<String, String>(1000, transactionManager)  // 1000개씩 처리
            .reader(wikiFileReader)
            .processor(simpleProcessor)
            .writer(simpleWriter)
            .build()
    }

    @Bean
    fun simpleProcessor(): ItemProcessor<String, String> {
        return ItemProcessor { item ->
            // TODO: 텍스트 처리 로직
            item?.uppercase()
        }
    }

    @Bean
    fun simpleWriter(): ItemWriter<String> {
        return ItemWriter { items ->
            // TODO: Kafka 또는 DB에 쓰기
            items.forEach { item ->
                println("Processing: $item")
            }
        }
    }
}