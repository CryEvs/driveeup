package ru.driveeup.mobile.domain

enum class UserRole { PASSENGER, DRIVER, ADMIN }

data class User(
    val id: Long,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val city: String = "",
    val role: UserRole,
    val driveCoin: Double,
    val totalDriveCoin: Double = 0.0,
    val premium: Boolean,
    val avatarUrl: String? = null,
    val ratingAvg: Double = 5.0,
    val ridesCount: Long = 0,
    val vehicleModel: String? = null,
    val vehiclePlate: String? = null
)

data class AuthResult(
    val accessToken: String,
    val user: User
)
