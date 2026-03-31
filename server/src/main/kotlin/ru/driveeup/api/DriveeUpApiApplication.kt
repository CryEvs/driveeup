package ru.driveeup.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DriveeUpApiApplication

fun main(args: Array<String>) {
    runApplication<DriveeUpApiApplication>(*args)
}
