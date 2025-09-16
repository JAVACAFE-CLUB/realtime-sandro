package com.sandro.realtime.codex

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class CodexApplication

fun main(args: Array<String>) {
    runApplication<CodexApplication>(*args)
}