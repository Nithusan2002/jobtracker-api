package com.nithusan.jobtracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@SpringBootApplication
class JobTrackerApplication

fun main(args: Array<String>) {
	configureDatasourceFromEnvironment()
	runApplication<JobTrackerApplication>(*args)
}

private fun configureDatasourceFromEnvironment() {
	val rawUrl = System.getenv("SPRING_DATASOURCE_URL")
		?: System.getenv("DATABASE_URL")
		?: return

	postgresConnectionProperties(rawUrl).forEach { (key, value) ->
		val overridden = when (key) {
			"spring.datasource.username" -> !System.getenv("SPRING_DATASOURCE_USERNAME").isNullOrBlank()
			"spring.datasource.password" -> !System.getenv("SPRING_DATASOURCE_PASSWORD").isNullOrBlank()
			else -> false
		}
		if (!overridden) System.setProperty(key, value)
	}
}

internal fun postgresConnectionProperties(rawUrl: String): Map<String, String> {
	if (!rawUrl.startsWith("postgres://") && !rawUrl.startsWith("postgresql://")) return emptyMap()

	val uri = URI(rawUrl)
	val port = if (uri.port == -1) "" else ":${uri.port}"
	val query = if (uri.rawQuery.isNullOrBlank()) "" else "?${uri.rawQuery}"
	val result = mutableMapOf("spring.datasource.url" to "jdbc:postgresql://${uri.host}$port${uri.path}$query")

	val userInfo = uri.rawUserInfo ?: return result
	val parts = userInfo.split(":", limit = 2)
	result["spring.datasource.username"] = decode(parts[0])
	if (parts.size == 2) result["spring.datasource.password"] = decode(parts[1])
	return result
}

private fun decode(value: String): String =
	URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8)
