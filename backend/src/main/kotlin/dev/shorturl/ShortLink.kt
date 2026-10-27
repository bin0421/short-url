package dev.shorturl

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Entity
@Table(name = "short_link")
class ShortLink(
    @Column(name = "target_url", nullable = false, length = 2048)
    val targetUrl: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val code: Long? = null
}

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {

    @Query(
        "SELECT * FROM short_link WHERE md5(target_url) = md5(:targetUrl) AND target_url = :targetUrl",
        nativeQuery = true,
    )
    fun findByTargetUrl(targetUrl: String): ShortLink?

    /** Returns the new Code, or null when another request already stored this Target URL. */
    @Transactional
    @Query(
        """
        INSERT INTO short_link (target_url, created_at) VALUES (:targetUrl, now())
        ON CONFLICT ((md5(target_url))) DO NOTHING
        RETURNING code
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(targetUrl: String): Long?
}

data class ShortLinkResult(val shortLink: ShortLink, val created: Boolean)

@Service
class ShortLinkService(private val shortLinks: ShortLinkRepository) {

    /** Returns the Short Link for a normalized Target URL, creating it only if none exists. */
    fun findOrCreate(targetUrl: String): ShortLinkResult {
        shortLinks.findByTargetUrl(targetUrl)?.let { return ShortLinkResult(it, created = false) }
        // Checked first so a resubmission doesn't consume a Code from the identity sequence.
        val code = shortLinks.insertIfAbsent(targetUrl)
            ?: return ShortLinkResult(shortLinks.findByTargetUrl(targetUrl)!!, created = false)
        return ShortLinkResult(shortLinks.findById(code).orElseThrow(), created = true)
    }

    fun find(code: Long): ShortLink? = shortLinks.findById(code).orElse(null)
}
