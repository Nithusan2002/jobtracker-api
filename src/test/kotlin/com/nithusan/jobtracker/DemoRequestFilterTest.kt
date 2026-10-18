package com.nithusan.jobtracker

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DemoRequestFilterTest {
    private fun filter(requests: Int = 120, writes: Int = 20) =
        DemoRequestFilter(jacksonObjectMapper().findAndRegisterModules(), requests, writes)

    private fun request(method: String = "GET", body: String = "") = MockHttpServletRequest(method, "/api/applications").apply {
        contentType = "application/json"
        setContent(body.toByteArray())
    }

    private val pass = FilterChain { _: ServletRequest, _: ServletResponse -> }

    @Test
    fun `rate budget resets after one minute without sleeping`() {
        val clock = MutableClock()
        val limiter = DemoRateLimiter(2, 1, clock)
        assertThat(limiter.retryAfterSeconds(true)).isZero()
        assertThat(limiter.retryAfterSeconds(true)).isEqualTo(60)
        assertThat(limiter.retryAfterSeconds(false)).isZero()
        clock.now = clock.now.plusSeconds(59)
        assertThat(limiter.retryAfterSeconds(false)).isEqualTo(1)
        clock.now = clock.now.plusSeconds(1)
        assertThat(limiter.retryAfterSeconds(true)).isZero()
    }

    @Test
    fun `write exhaustion does not block reads and includes retry header`() {
        val filter = filter(writes = 1)
        filter.doFilter(request("POST", "{}"), MockHttpServletResponse(), pass)
        val response = MockHttpServletResponse()
        filter.doFilter(request("POST", "{}"), response, pass)
        assertThat(response.status).isEqualTo(429)
        assertThat(response.getHeader("Retry-After")!!.toInt()).isBetween(1, 60)
        assertThat(response.contentAsString).contains("Too Many Requests")
        val read = MockHttpServletResponse()
        filter.doFilter(request(), read, pass)
        assertThat(read.status).isEqualTo(200)
    }

    @Test
    fun `forwarded addresses cannot bypass global budget`() {
        val filter = filter(requests = 1)
        filter.doFilter(request().apply { addHeader("X-Forwarded-For", "192.0.2.1") }, MockHttpServletResponse(), pass)
        val response = MockHttpServletResponse()
        filter.doFilter(request().apply { addHeader("X-Forwarded-For", "192.0.2.2") }, response, pass)
        assertThat(response.status).isEqualTo(429)
    }

    @Test
    fun `unknown content length still enforces body limit`() {
        val request = object : MockHttpServletRequest("POST", "/api/applications") {
            override fun getContentLength() = -1
            override fun getContentLengthLong() = -1L
        }.apply { setContent(ByteArray(DemoRequestFilter.MAX_BODY_BYTES + 1)) }
        val response = MockHttpServletResponse()
        var reachedController = false
        filter().doFilter(request, response, FilterChain { _, _ -> reachedController = true })
        assertThat(response.status).isEqualTo(413)
        assertThat(reachedController).isFalse()
    }

    @Test
    fun `buffering preserves utf8 json for the controller`() {
        val body = """{"companyName":"Fjell Demo AS","notes":"Ærlig øving"}"""
        val response = MockHttpServletResponse()
        var received = ""
        filter().doFilter(request("POST", body), response, FilterChain { req, _ ->
            received = req.inputStream.readAllBytes().toString(Charsets.UTF_8)
        })
        assertThat(received).isEqualTo(body)
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store")
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-18T00:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
}
