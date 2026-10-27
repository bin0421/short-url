package dev.shorturl

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals

@Import(TestcontainersConfiguration::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["app.public-base-url=https://sho.rt"],
)
class RateLimitHttpTest {

    @LocalServerPort
    private var port: Int = 0

    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
    private val json = JsonMapper.builder().build()

    // Caddy puts the visitor's IP in X-Forwarded-For; the limit must key on it, not on the proxy's address.
    private fun createShortLink(clientIp: String, targetUrl: String): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/api/links"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", clientIp)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(mapOf("targetUrl" to targetUrl))))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun follow(clientIp: String, code: Long): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/$code")).header("X-Forwarded-For", clientIp).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test
    fun `the 11th creation in a minute from one IP gets 429 while other IPs are unaffected`() {
        val statuses = (1..10).map { createShortLink("203.0.113.1", "https://limit.example/$it").statusCode() }
        assertEquals(List(10) { 201 }, statuses)

        val limited = createShortLink("203.0.113.1", "https://limit.example/11")
        assertEquals(429, limited.statusCode())
        assertEquals("application/problem+json", limited.headers().firstValue("Content-Type").orElse(null))
        assertEquals("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", json.readTree(limited.body())["detail"].asString())

        assertEquals(201, createShortLink("203.0.113.2", "https://limit.example/other-ip").statusCode())
    }

    @Test
    fun `following Short Links is not rate limited`() {
        val code = json.readTree(createShortLink("203.0.113.3", "https://limit.example/follow").body())["code"].asLong()

        val statuses = (1..30).map { follow("203.0.113.3", code).statusCode() }

        assertEquals(List(30) { 302 }, statuses)
    }
}
