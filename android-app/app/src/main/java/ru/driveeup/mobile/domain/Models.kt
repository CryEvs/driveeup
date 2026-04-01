package ru.driveeup.mobile.domain

enum class UserRole { PASSENGER, DRIVER, ADMIN }

data class User(
    val id: Long,
    val email: String,
    val role: UserRole,
    val driveCoin: Long,
    val totalDriveCoin: Long = 0,
    val premium: Boolean,
    val avatarUrl: String? = null
)

data class AuthResult(
    val accessToken: String,
    val user: User
)
