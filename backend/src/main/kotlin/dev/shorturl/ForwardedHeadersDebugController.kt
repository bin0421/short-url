package dev.shorturl

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// TEMPORARY: shows how the hosting proxy forwards client IPs. Remove once the rate-limit key is fixed.
@RestController
class ForwardedHeadersDebugController {

    @GetMapping("/api/debug/forwarded")
    fun forwarded(request: HttpServletRequest): Map<String, Any?> = mapOf(
        "remoteAddr" to request.remoteAddr,
        "headers" to request.headerNames.toList()
            .filter { it.lowercase().let { n -> "forward" in n || "ip" in n || n.startsWith("cf-") || n.startsWith("x-") } }
            .associateWith { request.getHeaders(it).toList() },
    )
}
