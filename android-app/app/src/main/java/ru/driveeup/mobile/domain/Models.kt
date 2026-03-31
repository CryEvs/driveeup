package ru.driveeup.mobile.domain

enum class UserRole { PASSENGER, DRIVER }

data class User(
    val id: Long,
    val email: String,
    val role: UserRole,
    val driveeCoin: Long,
    val premium: Boolean,
    val avatarUrl: String? = null
)
