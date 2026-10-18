package com.nithusan.jobtracker

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.ByteArrayInputStream
import java.time.Clock

@Component
@ConditionalOnProperty(name = ["jobtracker.demo.enabled"], havingValue = "true")
class DemoRequestFilter(
    private val objectMapper: ObjectMapper,
    @Value("\${jobtracker.demo.requests-per-minute:120}") requestsPerMinute: Int,
    @Value("\${jobtracker.demo.writes-per-minute:20}") writesPerMinute: Int
) : OncePerRequestFilter() {
    private val limiter = DemoRateLimiter(requestsPerMinute, writesPerMinute)

    override fun shouldNotFilter(request: HttpServletRequest) = !request.requestURI.removePrefix(request.contextPath).startsWith("/api/")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        response.setHeader("Cache-Control", "no-store")
        response.setHeader("X-Content-Type-Options", "nosniff")
        val write = request.method !in setOf("GET", "HEAD", "OPTIONS")
        if (write && request.getHeader("Sec-Fetch-Site") == "cross-site") {
            fail(response, 403, "Forbidden", "Endringer må gjøres fra demosiden.")
            return
        }
        val retryAfter = limiter.retryAfterSeconds(write)
        if (retryAfter > 0) {
            response.setHeader("Retry-After", retryAfter.toString())
            fail(response, 429, "Too Many Requests", "Demoen har mange forespørsler. Prøv igjen om $retryAfter sekunder.")
            return
        }
        if (write) {
            // Bound the stream itself, not just Content-Length (which may be absent).
            val bytes = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)
            if (bytes.size > MAX_BODY_BYTES) {
                fail(response, 413, "Payload Too Large", "Forespørselen er for stor. Maks 16 KB.")
                return
            }
            chain.doFilter(BufferedRequest(request, bytes), response)
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun fail(response: HttpServletResponse, code: Int, error: String, message: String) {
        response.status = code
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(response.writer, ErrorResponse(status = code, error = error, message = message))
    }

    companion object { const val MAX_BODY_BYTES = 16 * 1024 }
}

// One bounded, process-wide budget: no IP addresses or user identifiers are stored.
// Forwarded headers cannot create new buckets. Run one demo instance.
class DemoRateLimiter(
    private val maxRequests: Int,
    private val maxWrites: Int,
    private val clock: Clock = Clock.systemUTC()
) {
    private var windowStart = clock.instant().epochSecond
    private var requests = 0
    private var writes = 0

    @Synchronized
    fun retryAfterSeconds(write: Boolean): Long {
        val now = clock.instant().epochSecond
        if (now - windowStart >= 60) {
            windowStart = now
            requests = 0
            writes = 0
        }
        if (requests >= maxRequests || (write && writes >= maxWrites)) return (60 - (now - windowStart)).coerceAtLeast(1)
        requests++
        if (write) writes++
        return 0
    }
}

private class BufferedRequest(request: HttpServletRequest, private val bytes: ByteArray) : HttpServletRequestWrapper(request) {
    override fun getInputStream(): ServletInputStream {
        val input = ByteArrayInputStream(bytes)
        return object : ServletInputStream() {
            override fun read() = input.read()
            override fun isFinished() = input.available() == 0
            override fun isReady() = true
            override fun setReadListener(listener: ReadListener) = throw UnsupportedOperationException("Synchronous JSON requests only")
        }
    }
}
