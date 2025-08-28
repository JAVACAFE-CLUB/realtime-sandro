package com.sandro.realtime.harvest.batch

import com.sandro.realtime.harvest.config.FileConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.support.SynchronizedItemStreamReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import java.nio.file.Paths

/**
 * 라인 단위로 읽어서 Processor로 전달
 */
@Configuration
class WikiPageItemReader(
    private val fileConfig: FileConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun wikiFileReader(): ItemReader<String> {
        val wikiSource = fileConfig.sources.find { it.name == "kowiki" && it.enabled }
            ?: throw IllegalStateException("kowiki 소스가 설정되지 않았습니다")

        // ~ 를 홈 디렉토리로 변환
        val basePath = fileConfig.basePath.replace("~", System.getProperty("user.home"))
        val filePath = Paths.get(basePath, wikiSource.fileName)

        logger.info("Wiki 파일 읽기 설정:")
        logger.info("- 파일 경로: $filePath")
        logger.info("- 청크 크기: ${fileConfig.chunkSize}")
        logger.info("- 인코딩: ${fileConfig.encoding}")

        // 파일 존재 여부 확인
        val file = filePath.toFile()
        if (!file.exists()) {
            logger.error("파일이 존재하지 않습니다: $filePath")
            // 개발 환경에서는 더미 리더 반환
            return DummyItemReader()
        }

        logger.info("파일 크기: ${file.length() / 1024 / 1024} MB")

        // FlatFileItemReader를 사용하여 라인 단위로 읽기
        val reader = FlatFileItemReaderBuilder<String>()
            .name("wikiFileReader")
            .resource(FileSystemResource(file))
            .encoding(fileConfig.encoding)
            .lineMapper { line, lineNumber ->
                if (lineNumber % 10000 == 0) logger.info("$lineNumber 라인 처리 중...")
                line
            }
            .build()

        // 멀티스레드 환경에서 안전하게 사용하기 위해 동기화
        val synchronizedReader = SynchronizedItemStreamReader<String>()
        synchronizedReader.setDelegate(reader)

        return synchronizedReader
    }

    // 파일이 없을 때 사용할 더미 리더
    class DummyItemReader : ItemReader<String> {
        private var count = 0

        override fun read(): String? {
            if (count < 10) {
                count++
                return "더미 데이터 라인 #$count"
            }
            return null  // 더 이상 읽을 데이터가 없음
        }
    }
}