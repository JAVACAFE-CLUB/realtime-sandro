package com.sandro.realtime.harvest.batch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sandro.realtime.harvest.domain.WikiPage
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.springframework.batch.item.xml.StaxEventItemReader
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.ResourceLoader
import org.springframework.oxm.Unmarshaller
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.Source
import javax.xml.transform.stax.StAXSource

@Configuration
class WikiPageItemReaderConfig(
    private val resourceLoader: ResourceLoader
) {

    @Value("\${batch.wiki.file.path}")
    private lateinit var wikiFilePath: String

    @Bean
    fun wikiPageItemReader(): StaxEventItemReader<WikiPage> {
        val resource = resourceLoader.getResource(wikiFilePath)
        val inputStream = BZip2CompressorInputStream(
            BufferedInputStream(
                resource.inputStream,
                1024 * 1024  // 1MB 버퍼
            ),
            true  // decompressConcatenated: 연결된 bz2 블록 처리
        )

        return StaxEventItemReaderBuilder<WikiPage>()
            .name("wikiPageItemReader")
            .resource(InputStreamResource(inputStream))
            .addFragmentRootElements("page")
            .unmarshaller(JacksonXmlUnmarshaller())
            .build()
    }
}

class JacksonXmlUnmarshaller : Unmarshaller {

    private val xmlMapper = XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    }

    override fun supports(clazz: Class<*>): Boolean {
        return WikiPage::class.java.isAssignableFrom(clazz)
    }

    override fun unmarshal(source: Source): Any {
        return try {
            when (source) {
                is StAXSource -> {
                    val streamReader = source.xmlStreamReader
                    if (streamReader != null)
                        xmlMapper.readValue(streamReader, WikiPage::class.java)
                    else {
                        val bytes = writeEventsToByteArray(source.xmlEventReader)
                        ByteArrayInputStream(bytes).use { bais ->
                            xmlMapper.readValue(bais, WikiPage::class.java)
                        }
                    }
                }

                else -> {
                    throw UnsupportedOperationException("Unsupported source type: ${source::class.java}")
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Error unmarshalling WikiPage", e)
        }
    }

    private fun writeEventsToByteArray(eventReader: XMLEventReader): ByteArray {
        val baos = ByteArrayOutputStream(4096)
        val outputFactory = XMLOutputFactory.newFactory()
        val writer = outputFactory.createXMLEventWriter(baos, "UTF-8")
        try {
            while (eventReader.hasNext()) {
                val event = eventReader.nextEvent()
                writer.add(event)
            }
            writer.flush()
        } finally {
            writer.close()
        }
        return baos.toByteArray()
    }
}
