package com.anomot.anomotbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AnomotBackendApplication

fun main(args: Array<String>) {
    runApplication<AnomotBackendApplication>(*args)
}
