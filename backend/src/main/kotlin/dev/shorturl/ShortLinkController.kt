package dev.shorturl

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI

data class CreateShortLinkRequest(val targetUrl: String)

data class ShortLinkResponse(val code: Long, val shortUrl: String, val targetUrl: String)

@RestController
class ShortLinkController(
    private val shortLinks: ShortLinkService,
    private val rateLimiter: CreationRateLimiter,
    @Value("\${app.public-base-url}") private val publicBaseUrl: String,
) {

    private val serviceHost: String = URI(publicBaseUrl).host

    @PostMapping("/api/links")
    fun create(
        @RequestBody request: CreateShortLinkRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ShortLinkResponse> {
        // remoteAddr is the visitor's IP: forwarded headers from Caddy are applied (server.forward-headers-strategy).
        if (!rateLimiter.tryAcquire(servletRequest.remoteAddr)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
        }
        val targetUrl = when (val result = parseTargetUrl(request.targetUrl, serviceHost)) {
            is TargetUrlParseResult.Accepted -> result.targetUrl
            is TargetUrlParseResult.Rejected -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, result.reason.message)
        }
        val (shortLink, created) = shortLinks.findOrCreate(targetUrl)
        val code = shortLink.code!!
        return ResponseEntity.status(if (created) HttpStatus.CREATED else HttpStatus.OK)
            .body(ShortLinkResponse(code, "${publicBaseUrl.trimEnd('/')}/$code", shortLink.targetUrl))
    }

    @GetMapping("/{code:[0-9]+}")
    fun follow(@PathVariable code: String): ResponseEntity<Resource> {
        // A leading zero would give one Short Link several addresses; numbers past Long are never Codes.
        val shortLink = code.takeUnless { it.startsWith('0') }?.toLongOrNull()?.let(shortLinks::find)
            ?: return notFoundPage()
        return ResponseEntity.status(HttpStatus.FOUND).location(URI(shortLink.targetUrl)).build()
    }

    /** The SPA renders its not-found screen for any path other than the root. */
    private fun notFoundPage(): ResponseEntity<Resource> {
        val page = ClassPathResource("static/index.html")
        if (!page.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType("text", "html", Charsets.UTF_8))
            .body(page)
    }
}
