package com.sandro.realtime.smithy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmithyApplication

fun main(args: Array<String>) {
    runApplication<SmithyApplication>(*args)
}