package ru.driveeup.api.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-expiration-minutes}") private val accessExpirationMinutes: Long
) {
    fun generateToken(subject: String): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(accessExpirationMinutes * 60)
        val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun extractSubject(token: String): String =
        Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))).build()
            .parseSignedClaims(token)
            .payload
            .subject
}
