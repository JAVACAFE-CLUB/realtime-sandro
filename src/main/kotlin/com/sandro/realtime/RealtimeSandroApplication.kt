package com.sandro.realtime

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class RealtimeSandroApplication

fun main(args: Array<String>) {
    runApplication<RealtimeSandroApplication>(*args)
}
