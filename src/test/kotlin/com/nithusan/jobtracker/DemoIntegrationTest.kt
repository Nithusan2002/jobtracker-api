package com.nithusan.jobtracker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@SpringBootTest(properties = [
    "jobtracker.demo.enabled=true", "jobtracker.demo.reset-interval=PT24H",
    "jobtracker.demo.max-applications=6", "jobtracker.demo.requests-per-minute=10000",
    "jobtracker.demo.writes-per-minute=10000"
])
@AutoConfigureMockMvc
@Testcontainers
class DemoIntegrationTest {
    @Autowired lateinit var repository: ApplicationRepository
    @Autowired lateinit var demoData: DemoDataService
    @Autowired lateinit var mockMvc: MockMvc

    @BeforeEach
    fun reset() = demoData.reset()

    @Test
    fun `demo exposes mode and starts with explicitly fictitious samples`() {
        mockMvc.perform(get("/api/demo"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.resetIntervalSeconds").value(86400))
        assertThat(repository.findAll()).hasSize(5).allSatisfy {
            assertThat(it.notes).contains("Fiktiv")
            assertThat(it.jobListingUrl).isEqualTo("https://example.com/")
        }
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
    }

    @Test
    fun `reset replaces visitor data and restores deleted samples`() {
        repository.deleteAll()
        repository.save(Application(companyName = "Visitor Demo", jobTitle = "Test", applicationDate = LocalDate.now()))
        demoData.reset()
        assertThat(repository.findAll()).hasSize(5).noneMatch { it.companyName == "Visitor Demo" }
    }

    @Test
    fun `capacity returns 409 until a row is deleted`() {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
            .andExpect(status().isCreated)
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Demoen er full")))
        assertThat(repository.count()).isEqualTo(6)
        repository.deleteById(repository.findAll().first().id!!)
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
            .andExpect(status().isCreated)
    }

    @Test
    fun `concurrent creates cannot exceed demo capacity`() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val responses = executor.invokeAll((1..8).map {
                Callable {
                    mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                        .andReturn().response.status
                }
            }).map { it.get() }
            assertThat(responses.count { it == 201 }).isEqualTo(1)
            assertThat(responses.count { it == 409 }).isEqualTo(7)
            assertThat(repository.count()).isEqualTo(6)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `oversized body and cross-site writes are rejected without storing data`() {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content("x".repeat(17000)))
            .andExpect(status().isPayloadTooLarge)
        mockMvc.perform(post("/api/applications").header("Sec-Fetch-Site", "cross-site")
            .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
            .andExpect(status().isForbidden)
        assertThat(repository.count()).isEqualTo(5)
    }

    companion object {
        private const val PAYLOAD = """{"companyName":"Test Demo","jobTitle":"Backend","applicationDate":"2026-10-18","status":"SENT"}"""
        @Container @JvmField val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")
        @DynamicPropertySource @JvmStatic
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
