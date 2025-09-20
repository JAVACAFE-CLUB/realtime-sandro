package com.sandro.realtime.harvest.batch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sandro.realtime.harvest.domain.WikiPage
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ItemStreamReader
import java.io.BufferedInputStream
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * Wikipedia 덤프 파일을 위한 최적화된 ItemReader
 * XMLEventReader → ByteArray 변환 없이 XMLStreamReader를 직접 사용하여 성능 개선
 */
class OptimizedWikiPageItemReader(
    private val inputStream: InputStream,
    private val bufferSize: Int = 32 * 1024 * 1024  // 32MB 기본 버퍼
) : ItemStreamReader<WikiPage> {

    private lateinit var xmlStreamReader: XMLStreamReader
    private lateinit var decompressedStream: InputStream

    private val xmlMapper = XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    }

    private val xmlInputFactory = XMLInputFactory.newInstance().apply {
        // 성능 최적화 설정
        setProperty(XMLInputFactory.IS_COALESCING, true)  // 텍스트 노드 병합
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)  // 보안
        setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)  // 성능
        setProperty(XMLInputFactory.IS_VALIDATING, false)  // 성능
        setProperty(XMLInputFactory.SUPPORT_DTD, false)  // 보안 & 성능
    }

    private var currentItemCount = 0L
    private var hasMorePages = true

    companion object {
        private const val CURRENT_ITEM_COUNT_KEY = "current.item.count"
        private const val PAGE_ELEMENT = "page"
        private const val MEDIAWIKI_ELEMENT = "mediawiki"
    }

    override fun open(executionContext: ExecutionContext) {
        try {
            // BZip2 압축 해제 스트림 설정
            decompressedStream = BZip2CompressorInputStream(
                BufferedInputStream(inputStream, bufferSize),
                true  // decompressConcatenated: 연결된 bz2 블록 처리
            )

            // XMLStreamReader 생성
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(decompressedStream, Charsets.UTF_8.name())

            // ExecutionContext에서 이전 상태 복원 (재시작 지원)
            if (executionContext.containsKey(CURRENT_ITEM_COUNT_KEY)) {
                currentItemCount = executionContext.getLong(CURRENT_ITEM_COUNT_KEY)
                skipToItem(currentItemCount)
            } else {
                // 첫 <page> 요소로 이동
                moveToFirstPage()
            }

        } catch (e: Exception) {
            throw ItemStreamException("Failed to open reader", e)
        }
    }

    override fun read(): WikiPage? {
        if (!hasMorePages) {
            return null
        }

        return try {
            // 현재 위치가 <page> 요소인지 확인
            if (xmlStreamReader.eventType == XMLStreamConstants.START_ELEMENT
                && xmlStreamReader.localName == PAGE_ELEMENT
            ) {

                // XMLStreamReader를 직접 사용하여 WikiPage 객체로 변환
                val wikiPage = xmlMapper.readValue(xmlStreamReader, WikiPage::class.java)
                currentItemCount++

                // 다음 <page> 요소로 이동
                moveToNextPage()

                wikiPage
            } else {
                // <page> 요소가 아닌 경우 다음 페이지 찾기
                if (moveToNextPage()) {
                    read()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            throw ItemStreamException("Failed to read item", e)
        }
    }

    override fun update(executionContext: ExecutionContext) {
        // 현재 진행 상태 저장 (재시작 지원)
        executionContext.putLong(CURRENT_ITEM_COUNT_KEY, currentItemCount)
    }

    override fun close() {
        try {
            if (::xmlStreamReader.isInitialized) {
                xmlStreamReader.close()
            }
            if (::decompressedStream.isInitialized) {
                decompressedStream.close()
            }
            inputStream.close()

        } catch (e: Exception) {
            throw ItemStreamException("Failed to close reader", e)
        }
    }

    /**
     * 첫 번째 <page> 요소로 이동
     */
    private fun moveToFirstPage() {
        while (xmlStreamReader.hasNext()) {
            val eventType = xmlStreamReader.next()

            if (eventType == XMLStreamConstants.START_ELEMENT) {
                when (xmlStreamReader.localName) {
                    MEDIAWIKI_ELEMENT -> {}
                    PAGE_ELEMENT -> {
                        return
                    }
                }
            }
        }
        hasMorePages = false
    }

    /**
     * 다음 <page> 요소로 이동
     * @return 다음 페이지를 찾았으면 true, 더 이상 페이지가 없으면 false
     */
    private fun moveToNextPage(): Boolean {
        while (xmlStreamReader.hasNext()) {
            val eventType = xmlStreamReader.next()

            if (eventType == XMLStreamConstants.START_ELEMENT
                && xmlStreamReader.localName == PAGE_ELEMENT
            ) {
                return true
            }

            // </mediawiki> 종료 태그를 만나면 더 이상 페이지 없음
            if (eventType == XMLStreamConstants.END_ELEMENT
                && xmlStreamReader.localName == MEDIAWIKI_ELEMENT
            ) {
                hasMorePages = false
                return false
            }
        }

        hasMorePages = false
        return false
    }

    /**
     * 재시작 시 특정 아이템 위치로 스킵
     */
    private fun skipToItem(targetCount: Long) {
        moveToFirstPage()

        var skippedCount = 0L
        while (skippedCount < targetCount && hasMorePages) {
            if (moveToNextPage()) {
                skippedCount++
            } else {
                break
            }
        }
    }
}
