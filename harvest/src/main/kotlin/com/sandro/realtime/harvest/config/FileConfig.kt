package com.sandro.realtime.harvest.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "harvest.file")
data class FileConfig(
    var basePath: String = "~/bigdata",
    var sources: List<FileSource> = listOf(),
    var chunkSize: Int = 1048576,  // 1MB
    var encoding: String = "UTF-8"
) {
    data class FileSource(
        var name: String = "",
        var fileName: String = "",
        var enabled: Boolean = true
    )
}