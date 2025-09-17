package com.sandro.realtime.harvest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HarvestApplication

fun main(args: Array<String>) {
    runApplication<HarvestApplication>(*args)
}