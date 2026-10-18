package com.nithusan.jobtracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

class DemoCapacityException : RuntimeException("Demoen er full. Slett en demosøknad eller prøv igjen etter neste tilbakestilling.")

@Component
class DemoPolicy(
    private val jdbc: JdbcTemplate,
    private val repository: ApplicationRepository,
    @Value("\${jobtracker.demo.enabled:false}") val enabled: Boolean,
    @Value("\${jobtracker.demo.max-applications:100}") private val maxApplications: Int
) {
    // Shared by writes and resets. A PostgreSQL transaction lock also works
    // across application instances and is released only when the transaction ends.
    fun lockWrites() {
        if (enabled) jdbc.execute("SELECT pg_advisory_xact_lock(73489201)")
    }

    fun checkCapacity() {
        if (enabled && repository.count() >= maxApplications) throw DemoCapacityException()
    }
}
