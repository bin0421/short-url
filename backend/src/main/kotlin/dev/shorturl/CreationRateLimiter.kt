package dev.shorturl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Limits Short Link creation per client IP (ADR 0002). In memory, because the service runs as a single instance.
 */
@Component
class CreationRateLimiter(
    @Value("\${app.rate-limit.creations-per-minute}") private val creationsPerMinute: Long,
) {
    // A bucket idle for a full minute has refilled completely, so evicting it loses nothing.
    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build()

    fun tryAcquire(clientIp: String): Boolean =
        buckets.get(clientIp) { newBucket() }.tryConsume(1)

    private fun newBucket(): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(creationsPerMinute)
                .refillGreedy(creationsPerMinute, Duration.ofMinutes(1))
                .build(),
        )
        .build()
}
