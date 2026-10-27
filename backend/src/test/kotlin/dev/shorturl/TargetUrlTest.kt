package dev.shorturl

import kotlin.test.Test
import kotlin.test.assertEquals

class TargetUrlTest {

    private fun parse(input: String) = parseTargetUrl(input, serviceHost = "sho.rt")

    private fun assertAccepted(expected: String, input: String) =
        assertEquals(TargetUrlParseResult.Accepted(expected), parse(input))

    private fun assertRejected(reason: TargetUrlRejection, input: String) =
        assertEquals(TargetUrlParseResult.Rejected(reason), parse(input))

    @Test
    fun `accepts http and https Target URLs unchanged`() {
        assertAccepted("https://example.com/docs?page=1", "https://example.com/docs?page=1")
        assertAccepted("http://example.com", "http://example.com")
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertAccepted("https://example.com/a", "  https://example.com/a \n")
    }

    @Test
    fun `lowercases only the scheme and host`() {
        assertAccepted("https://example.com/Path?Q=A#Frag", "HTTPS://Example.COM/Path?Q=A#Frag")
    }

    @Test
    fun `keeps path, trailing slash, port and user info as given`() {
        assertAccepted("https://example.com/", "https://example.com/")
        assertAccepted("https://example.com", "https://example.com")
        assertAccepted("http://user@example.com:8080/a%20b", "http://user@Example.com:8080/a%20b")
    }

    @Test
    fun `rejects blank input`() {
        assertRejected(TargetUrlRejection.BLANK, "")
        assertRejected(TargetUrlRejection.BLANK, "   ")
    }

    @Test
    fun `rejects schemes other than http and https`() {
        assertRejected(TargetUrlRejection.UNSUPPORTED_SCHEME, "ftp://example.com/file")
        assertRejected(TargetUrlRejection.UNSUPPORTED_SCHEME, "javascript:alert(1)")
        assertRejected(TargetUrlRejection.UNSUPPORTED_SCHEME, "example.com/no-scheme")
    }

    @Test
    fun `rejects text that is not a URL or has no host`() {
        assertRejected(TargetUrlRejection.MALFORMED, "https://exa mple.com")
        assertRejected(TargetUrlRejection.MALFORMED, "https:///path-only")
        assertRejected(TargetUrlRejection.MALFORMED, "http:example.com")
    }

    @Test
    fun `rejects links to the service itself`() {
        assertRejected(TargetUrlRejection.SERVICE_HOST, "https://sho.rt/1")
        assertRejected(TargetUrlRejection.SERVICE_HOST, "http://SHO.RT/1")
    }

    @Test
    fun `accepts exactly 2048 characters and rejects 2049`() {
        val prefix = "https://example.com/"
        val atLimit = prefix + "a".repeat(2048 - prefix.length)

        assertAccepted(atLimit, atLimit)
        assertRejected(TargetUrlRejection.TOO_LONG, atLimit + "a")
    }
}
