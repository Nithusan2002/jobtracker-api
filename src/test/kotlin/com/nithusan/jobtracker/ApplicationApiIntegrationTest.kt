package com.nithusan.jobtracker

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationApiIntegrationTest {
	@Autowired
	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var objectMapper: ObjectMapper

	@Autowired
	lateinit var repository: ApplicationRepository

	@BeforeEach
	fun clearDatabase() {
		// Includes demo data seeded at startup; never connects to the developer's database.
		repository.deleteAll()
	}

	@Test
	fun `create returns generated id and persists the application`() {
		val response = mockMvc.perform(
			post(API).contentType(MediaType.APPLICATION_JSON)
				.content(json(payload() + ("id" to 999999L)))
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").isNumber)
			.andExpect(jsonPath("$.companyName").value("Demo AS"))
			.andExpect(jsonPath("$.jobTitle").value("Backend-utvikler"))
			.andExpect(jsonPath("$.applicationDate").value("2026-08-11"))
			.andExpect(jsonPath("$.status").value("SENT"))
			.andExpect(jsonPath("$.jobListingUrl").value("https://example.com/job/123"))
			.andExpect(jsonPath("$.notes").value("Følg opp neste uke"))
			.andReturn().response.contentAsString

		val id = objectMapper.readTree(response)["id"].asLong()
		assertThat(id).isNotEqualTo(999999L)
		assertThat(repository.count()).isEqualTo(1L)
		val stored = repository.findById(id).orElseThrow()
		assertThat(stored.companyName).isEqualTo("Demo AS")
		assertThat(stored.applicationDate).isEqualTo(LocalDate.of(2026, 8, 11))
	}

	@Test
	fun `list returns an empty array when no applications exist`() {
		mockMvc.perform(get(API))
			.andExpect(status().isOk)
			.andExpect(content().json("[]"))
	}

	@Test
	fun `list and get return stored applications`() {
		val stored = saveApplication()
		mockMvc.perform(get(API))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(stored.id))
			.andExpect(jsonPath("$[0].companyName").value("Demo AS"))

		mockMvc.perform(get("$API/${stored.id}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(stored.id))
			.andExpect(jsonPath("$.companyName").value("Demo AS"))
	}

	@Test
	fun `update changes persisted fields and preserves the path id`() {
		val stored = saveApplication()
		val changes = payload() + mapOf(
			"id" to 999999L,
			"companyName" to "Ny bedrift AS",
			"jobTitle" to "Kotlin-utvikler",
			"applicationDate" to "2026-08-12",
			"status" to "INTERVIEW",
			"jobListingUrl" to null,
			"notes" to null
		)
		mockMvc.perform(
			put("$API/${stored.id}").contentType(MediaType.APPLICATION_JSON).content(json(changes))
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(stored.id))
			.andExpect(jsonPath("$.status").value("INTERVIEW"))

		val updated = repository.findById(stored.id!!).orElseThrow()
		assertThat(updated.companyName).isEqualTo("Ny bedrift AS")
		assertThat(updated.jobTitle).isEqualTo("Kotlin-utvikler")
		assertThat(updated.applicationDate).isEqualTo(LocalDate.of(2026, 8, 12))
		assertThat(updated.status).isEqualTo(ApplicationStatus.INTERVIEW)
		assertThat(updated.jobListingUrl).isNull()
		assertThat(updated.notes).isNull()
		assertThat(repository.count()).isEqualTo(1L)
	}

	@Test
	fun `delete removes the application and returns no content`() {
		val stored = saveApplication()
		mockMvc.perform(delete("$API/${stored.id}"))
			.andExpect(status().isNoContent)
			.andExpect(content().string(""))
		assertThat(repository.existsById(stored.id!!)).isFalse()
		mockMvc.perform(get("$API/${stored.id}"))
			.andExpect(status().isNotFound)
	}

	@ParameterizedTest
	@ValueSource(strings = ["GET", "PUT", "DELETE"])
	fun `unknown id returns a structured 404 without creating data`(method: String) {
		val request = request(HttpMethod.valueOf(method), "$API/999999")
		if (method == "PUT") request.contentType(MediaType.APPLICATION_JSON).content(json(payload()))
		mockMvc.perform(request)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value("Fant ingen søknad med id 999999"))
			.andExpect(jsonPath("$.timestamp").exists())
		assertThat(repository.count()).isZero()
	}

	@ParameterizedTest
	@MethodSource("invalidFields")
	fun `invalid fields return structured 400 and leave stored data unchanged`(field: String, value: Any?) {
		val stored = saveApplication()
		val invalid = json(payload() + (field to value))
		for (request in listOf(post(API), put("$API/${stored.id}"))) {
			mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest)
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validering feilet"))
				.andExpect(jsonPath("$.fieldErrors.$field").isNotEmpty())
		}
		assertThat(repository.findAll()).containsExactly(stored)
	}

	@ParameterizedTest
	@MethodSource("malformedRequests")
	fun `malformed input returns 400 without changing data`(body: String) {
		val stored = saveApplication()
		for (request in listOf(post(API), put("$API/${stored.id}"))) {
			mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest)
		}
		assertThat(repository.findAll()).containsExactly(stored)
	}

	@Test
	fun `missing application date is rejected without creating data`() {
		mockMvc.perform(
			post(API).contentType(MediaType.APPLICATION_JSON).content(json(payload() - "applicationDate"))
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.fieldErrors.applicationDate").isNotEmpty())
		assertThat(repository.count()).isZero()
	}

	private fun saveApplication(): Application = repository.save(
		Application(companyName = "Demo AS", jobTitle = "Backend-utvikler", applicationDate = LocalDate.of(2026, 8, 11))
	)

	private fun json(value: Any): String = objectMapper.writeValueAsString(value)

	companion object {
		private const val API = "/api/applications"

		@Container
		@JvmField
		val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")

		@DynamicPropertySource
		@JvmStatic
		fun databaseProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url") { postgres.jdbcUrl }
			registry.add("spring.datasource.username") { postgres.username }
			registry.add("spring.datasource.password") { postgres.password }
		}

		private fun payload(): Map<String, Any?> = mapOf(
			"companyName" to "Demo AS",
			"jobTitle" to "Backend-utvikler",
			"applicationDate" to "2026-08-11",
			"status" to "SENT",
			"jobListingUrl" to "https://example.com/job/123",
			"notes" to "Følg opp neste uke"
		)

		@JvmStatic
		fun invalidFields(): Stream<Arguments> = Stream.of(
			Arguments.of("companyName", ""),
			Arguments.of("companyName", "   "),
			Arguments.of("jobTitle", ""),
			Arguments.of("jobTitle", "   "),
			Arguments.of("applicationDate", null)
		)

		@JvmStatic
		fun malformedRequests(): Stream<String> = Stream.of(
			"{",
			"""{"companyName":"Demo AS","jobTitle":"Backend","applicationDate":"2026-02-30","status":"SENT"}""",
			"""{"companyName":"Demo AS","jobTitle":"Backend","applicationDate":"2026-08-11","status":"UNKNOWN"}"""
		)
	}
}
