package ru.driveeup.api.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemController {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok", "service" to "driveeup-api")
}
