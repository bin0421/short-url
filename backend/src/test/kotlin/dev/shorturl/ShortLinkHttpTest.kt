package dev.shorturl

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["app.public-base-url=https://sho.rt", "app.rate-limit.creations-per-minute=1000"],
)
class ShortLinkHttpTest {

    @LocalServerPort
    private var port: Int = 0

    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
    private val json = JsonMapper.builder().build()

    private fun createShortLink(targetUrl: String): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/api/links"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(mapOf("targetUrl" to targetUrl))))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun follow(path: String): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test
    fun `creating a Short Link returns 201 with its Code and short URL`() {
        val response = createShortLink("https://example.com/docs?page=1")

        assertEquals(201, response.statusCode())
        val body = json.readTree(response.body())
        val code = body["code"].asLong()
        assertTrue(code >= 1)
        assertEquals("https://sho.rt/$code", body["shortUrl"].asString())
        assertEquals("https://example.com/docs?page=1", body["targetUrl"].asString())
    }

    @Test
    fun `following a Short Link redirects to its Target URL with 302`() {
        val code = json.readTree(createShortLink("https://example.org/a/b").body())["code"].asLong()

        val response = follow("/$code")

        assertEquals(302, response.statusCode())
        assertEquals("https://example.org/a/b", response.headers().firstValue("Location").orElse(null))
    }

    @Test
    fun `the created Short Link carries the normalized Target URL`() {
        val response = createShortLink("  HTTPS://Example.COM/Docs  ")

        assertEquals(201, response.statusCode())
        assertEquals("https://example.com/Docs", json.readTree(response.body())["targetUrl"].asString())
    }

    @Test
    fun `resubmitting a Target URL returns the existing Short Link with 200`() {
        val first = createShortLink("https://reuse.example/page")
        val second = createShortLink("https://reuse.example/page")

        assertEquals(201, first.statusCode())
        assertEquals(200, second.statusCode())
        assertEquals(json.readTree(first.body()), json.readTree(second.body()))
    }

    @Test
    fun `a Target URL differing only in scheme or host case reuses the Short Link`() {
        val first = json.readTree(createShortLink("https://Case.Example/Path").body())
        val second = createShortLink("HTTPS://case.example/Path")

        assertEquals(200, second.statusCode())
        assertEquals(first["code"].asLong(), json.readTree(second.body())["code"].asLong())
    }

    @Test
    fun `Target URLs differing in path, query or trailing slash get different Codes`() {
        val codes = listOf(
            "https://distinct.example",
            "https://distinct.example/",
            "https://distinct.example/Path",
            "https://distinct.example/path",
            "https://distinct.example/path?x=1",
        ).map { json.readTree(createShortLink(it).body())["code"].asLong() }

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `concurrent submissions of one Target URL create a single Short Link`() {
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val responses = try {
            (1..8).map {
                executor.submit<HttpResponse<String>> {
                    start.await()
                    createShortLink("https://concurrent.example/same")
                }
            }.also { start.countDown() }.map { it.get() }
        } finally {
            executor.shutdown()
        }

        assertEquals(List(7) { 200 } + 201, responses.map { it.statusCode() }.sorted())
        assertEquals(1, responses.map { json.readTree(it.body())["code"].asLong() }.toSet().size)
    }

    private fun assertNotFoundPage(response: HttpResponse<String>) {
        assertEquals(404, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"))
        assertTrue(response.body().contains("<div id=\"root\">"))
    }

    @Test
    fun `an unknown Code gets the not-found page with 404`() {
        assertNotFoundPage(follow("/987654321"))
    }

    @Test
    fun `a Code written with a leading zero gets 404 even when the Code exists`() {
        val code = json.readTree(createShortLink("https://leading-zero.example").body())["code"].asLong()

        assertNotFoundPage(follow("/0$code"))
    }

    @Test
    fun `a number beyond the Code range gets 404 instead of an error`() {
        assertNotFoundPage(follow("/99999999999999999999"))
    }

    @Test
    fun `the root serves the built frontend page`() {
        val response = follow("/")

        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"))
        assertTrue(response.body().contains("<div id=\"root\">"))
    }

    @Test
    fun `frontend assets are served and not taken for Codes`() {
        val response = follow("/assets/index-test.js")

        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("javascript"))
    }

    @ParameterizedTest
    @CsvSource(
        "'   ', 줄일 URL을 입력해 주세요.",
        "'https://exa mple.com', 올바른 URL 형식이 아닙니다.",
        "'ftp://example.com/file', http:// 또는 https://로 시작하는 주소만 줄일 수 있습니다.",
        "'https://sho.rt/1', 이 서비스의 주소는 다시 줄일 수 없습니다.",
    )
    fun `a rejected Target URL gets 400 with the reason as a problem detail`(targetUrl: String, detail: String) {
        assertProblem(createShortLink(targetUrl), detail)
    }

    @Test
    fun `a Target URL longer than 2048 characters gets 400`() {
        assertProblem(createShortLink("https://example.com/" + "a".repeat(2029)), "URL은 2048자 이하여야 합니다.")
    }

    private fun assertProblem(response: HttpResponse<String>, detail: String) {
        assertEquals(400, response.statusCode())
        assertEquals("application/problem+json", response.headers().firstValue("Content-Type").orElse(null))
        assertEquals(detail, json.readTree(response.body())["detail"].asString())
    }
}
