package ru.driveeup.api.dto

import ru.driveeup.api.entity.UserRole

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: UserRole
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val role: UserRole,
    val driveeCoin: Long,
    val premium: Boolean
)
