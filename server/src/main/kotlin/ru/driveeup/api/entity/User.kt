package ru.driveeup.api.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.PASSENGER,

    @Column(nullable = false)
    var driveeCoin: Long = 0,

    @Column(nullable = false)
    var premium: Boolean = false,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)
