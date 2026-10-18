package com.nithusan.jobtracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class DemoController(
    private val policy: DemoPolicy,
    @Value("\${jobtracker.demo.reset-interval:PT30M}") private val resetInterval: Duration
) {
    @GetMapping("/api/demo")
    fun info() = mapOf(
        "enabled" to policy.enabled,
        "resetIntervalSeconds" to if (policy.enabled) resetInterval.seconds else 0
    )
}
