package dev.shorturl

import java.net.URI
import java.net.URISyntaxException

const val TARGET_URL_MAX_LENGTH = 2048

private val ALLOWED_SCHEMES = setOf("http", "https")

enum class TargetUrlRejection(val message: String) {
    BLANK("줄일 URL을 입력해 주세요."),
    MALFORMED("올바른 URL 형식이 아닙니다."),
    UNSUPPORTED_SCHEME("http:// 또는 https://로 시작하는 주소만 줄일 수 있습니다."),
    TOO_LONG("URL은 ${TARGET_URL_MAX_LENGTH}자 이하여야 합니다."),
    SERVICE_HOST("이 서비스의 주소는 다시 줄일 수 없습니다."),
}

sealed interface TargetUrlParseResult {
    data class Accepted(val targetUrl: String) : TargetUrlParseResult
    data class Rejected(val reason: TargetUrlRejection) : TargetUrlParseResult
}

/**
 * Validates a submitted Target URL and normalizes it: trims whitespace and lowercases the scheme and host,
 * leaving everything else as given, since servers may interpret path, query and trailing slash differently.
 */
fun parseTargetUrl(input: String, serviceHost: String): TargetUrlParseResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return TargetUrlParseResult.Rejected(TargetUrlRejection.BLANK)

    val uri = try {
        URI(trimmed)
    } catch (e: URISyntaxException) {
        return TargetUrlParseResult.Rejected(TargetUrlRejection.MALFORMED)
    }
    val scheme = uri.scheme?.lowercase()
    if (scheme !in ALLOWED_SCHEMES) return TargetUrlParseResult.Rejected(TargetUrlRejection.UNSUPPORTED_SCHEME)
    val host = uri.host?.lowercase() ?: return TargetUrlParseResult.Rejected(TargetUrlRejection.MALFORMED)
    if (host == serviceHost.lowercase()) return TargetUrlParseResult.Rejected(TargetUrlRejection.SERVICE_HOST)

    val normalized = buildString {
        append(scheme).append("://")
        uri.rawUserInfo?.let { append(it).append('@') }
        append(host)
        if (uri.port != -1) append(':').append(uri.port)
        append(uri.rawPath.orEmpty())
        uri.rawQuery?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }
    if (normalized.length > TARGET_URL_MAX_LENGTH) return TargetUrlParseResult.Rejected(TargetUrlRejection.TOO_LONG)

    return TargetUrlParseResult.Accepted(normalized)
}
