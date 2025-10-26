package com.sandro.realtime.harvest.wiki.batch.config

import com.sandro.realtime.harvest.wiki.batch.OptimizedWikiPageItemReader
import com.sandro.realtime.harvest.wiki.batch.WikiPageWriter
import com.sandro.realtime.harvest.wiki.domain.WikiPage
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.transaction.PlatformTransactionManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

@Configuration
class WikiBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val resourceLoader: ResourceLoader,
    private val wikiPageWriter: WikiPageWriter
) {
    @Value("\${batch.wiki.file.directory}")
    private lateinit var wikiFileDirectory: String

    @Value("\${batch.wiki.file.pattern}")
    private lateinit var wikiFilePattern: String

    @Value("\${batch.chunk.size:1000}") // TODO: 적절하게 튜닝하기
    private var chunkSize: Int = 1000

    // TODO: 트리거 설정하기
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
            .reader(optimizedWikiPageItemReader())
            .writer(wikiPageWriter)
            .build()
    }

    @Bean
    fun optimizedWikiPageItemReader(): ItemStreamReader<WikiPage> {
        val wikiFilePath = findLatestWikiFile()
        val resource = resourceLoader.getResource("file:$wikiFilePath")
        return OptimizedWikiPageItemReader(resource.inputStream)
    }

    /**
     * 디렉토리에서 패턴에 맞는 파일들 중 가장 최근(파일명이 가장 큰) 파일을 찾는다.
     * 파일명에 날짜가 포함되어 있으므로 사전순 정렬시 가장 최근 날짜의 파일이 마지막에 위치한다.
     */
    private fun findLatestWikiFile(): Path {
        val directory = Path.of(wikiFileDirectory)
        require(Files.exists(directory) && Files.isDirectory(directory)) {
            "위키 파일 디렉토리가 존재하지 않습니다: $wikiFileDirectory"
        }

        val pathMatcher = directory.fileSystem.getPathMatcher("glob:$wikiFilePattern")

        val latestFile = Files.list(directory)
            .filter { it.isRegularFile() }
            .filter { pathMatcher.matches(it.fileName) }
            .toList()
            .maxByOrNull { it.name }
            ?: throw IllegalStateException(
                "패턴 '$wikiFilePattern'에 맞는 위키 파일을 찾을 수 없습니다. 디렉토리: $wikiFileDirectory"
            )

        return latestFile
    }
}