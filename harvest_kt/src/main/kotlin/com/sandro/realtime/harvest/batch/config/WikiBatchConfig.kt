package com.sandro.realtime.harvest.batch.config

import com.sandro.realtime.harvest.batch.WikiPageWriter
import com.sandro.realtime.harvest.domain.WikiPage
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.xml.StaxEventItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class WikiBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val wikiPageItemReader: StaxEventItemReader<WikiPage>,
    private val wikiPageWriter: WikiPageWriter
) {

    @Value("\${batch.chunk.size:100}")
    private var chunkSize: Int = 100

    @Bean
    fun wikiProcessingJob(): Job {
        return JobBuilder("wikiProcessingJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(wikiPageProcessingStep())
            .build()
    }

    @Bean
    fun wikiPageProcessingStep(): Step {
        return StepBuilder("wikiPageProcessingStep", jobRepository)
            .chunk<WikiPage, WikiPage>(chunkSize, transactionManager)
            .reader(wikiPageItemReader)
            .writer(wikiPageWriter)
            .build()
    }
}