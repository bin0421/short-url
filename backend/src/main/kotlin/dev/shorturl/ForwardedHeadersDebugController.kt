package dev.shorturl

import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// TEMPORARY: shows how the hosting proxy forwards client IPs. Remove once the rate-limit key is fixed.
@RestController
class ForwardedHeadersDebugController {

    @GetMapping("/api/debug/forwarded")
    fun forwarded(request: HttpServletRequest): Map<String, Any?> {
        // ForwardedHeaderFilter hides X-Forwarded-* behind a wrapper; read the container's original request.
        var raw: ServletRequest = request
        while (raw is HttpServletRequestWrapper) raw = raw.request
        raw as HttpServletRequest
        return mapOf(
            "resolvedRemoteAddr" to request.remoteAddr,
            "rawRemoteAddr" to raw.remoteAddr,
            "headers" to raw.headerNames.toList()
                .filter { it.lowercase().let { n -> "forward" in n || "ip" in n || n.startsWith("cf-") || n.startsWith("x-") } }
                .associateWith { raw.getHeaders(it).toList() },
        )
    }
}
