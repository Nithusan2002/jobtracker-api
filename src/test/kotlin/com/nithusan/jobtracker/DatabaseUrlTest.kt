package com.nithusan.jobtracker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatabaseUrlTest {
    @Test
    fun `external postgres url preserves ssl and decodes credentials exactly once`() {
        val properties = postgresConnectionProperties("postgresql://demo:p%25%2B+%40%3A@db.example.com:6543/jobtracker?sslmode=require")
        assertThat(properties["spring.datasource.url"]).isEqualTo("jdbc:postgresql://db.example.com:6543/jobtracker?sslmode=require")
        assertThat(properties["spring.datasource.username"]).isEqualTo("demo")
        assertThat(properties["spring.datasource.password"]).isEqualTo("p%++@:")
    }

    @Test
    fun `jdbc configuration is left untouched`() {
        assertThat(postgresConnectionProperties("jdbc:postgresql://localhost:5432/jobtracker")).isEmpty()
    }
}
