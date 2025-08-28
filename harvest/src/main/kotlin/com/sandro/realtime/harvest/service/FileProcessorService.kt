package com.sandro.realtime.harvest.service

import com.sandro.realtime.harvest.config.FileConfig
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class FileProcessorService(
    private val fileConfig: FileConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        logger.info("파일 처리 서비스 초기화")
        logger.info("Base Path: ${fileConfig.basePath}")
        logger.info("Chunk Size: ${fileConfig.chunkSize} bytes")
        logger.info("Encoding: ${fileConfig.encoding}")

        fileConfig.sources.forEach { source ->
            if (source.enabled) {
                logger.info("활성화된 소스: ${source.name} - ${source.fileName}")
            }
        }
    }

    fun processFile(sourceName: String) {
        val source = fileConfig.sources.find { it.name == sourceName }
            ?: throw IllegalArgumentException("소스를 찾을 수 없음: $sourceName")

        if (!source.enabled) {
            logger.warn("비활성화된 소스: $sourceName")
            return
        }

        val filePath = Paths.get(fileConfig.basePath, source.fileName)

        if (!Files.exists(filePath)) {
            logger.error("파일이 존재하지 않음: $filePath")
            return
        }

        logger.info("파일 처리 시작: $filePath")
        logger.info("파일 크기: ${Files.size(filePath)} bytes")

        // TODO: 실제 파일 스트리밍 처리 구현
        processFileStream(filePath)
    }

    private fun processFileStream(filePath: Path) {
        Files.newInputStream(filePath).buffered(fileConfig.chunkSize).use { stream ->
            // 청크 단위로 읽기
            val buffer = ByteArray(fileConfig.chunkSize)
            var bytesRead: Int
            var chunkNumber = 0

            while (stream.read(buffer).also { bytesRead = it } != -1) {
                chunkNumber++
                logger.debug("청크 #$chunkNumber 처리중 (${bytesRead} bytes)")

                // TODO: Kafka로 전송
                // kafkaProducer.send(buffer.sliceArray(0 until bytesRead))
            }

            logger.info("파일 처리 완료: 총 $chunkNumber 청크")
        }
    }
}