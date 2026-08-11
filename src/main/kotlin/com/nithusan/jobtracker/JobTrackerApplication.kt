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

	if (!rawUrl.startsWith("postgres://") && !rawUrl.startsWith("postgresql://")) {
		return
	}

	val uri = URI(rawUrl)
	val port = if (uri.port == -1) "" else ":${uri.port}"
	val query = if (uri.query.isNullOrBlank()) "" else "?${uri.query}"
	System.setProperty("spring.datasource.url", "jdbc:postgresql://${uri.host}$port${uri.path}$query")

	val userInfo = uri.userInfo ?: return
	val parts = userInfo.split(":", limit = 2)
	if (System.getenv("SPRING_DATASOURCE_USERNAME").isNullOrBlank() && parts.isNotEmpty()) {
		System.setProperty("spring.datasource.username", decode(parts[0]))
	}
	if (System.getenv("SPRING_DATASOURCE_PASSWORD").isNullOrBlank() && parts.size == 2) {
		System.setProperty("spring.datasource.password", decode(parts[1]))
	}
}

private fun decode(value: String): String =
	URLDecoder.decode(value, StandardCharsets.UTF_8)
