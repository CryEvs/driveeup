package ru.driveeup.mobile.domain

data class RideUserBrief(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val avatarUrl: String?,
    val ratingAvg: Double,
    val ridesCount: Long,
    val vehicleModel: String?,
    val vehiclePlate: String?
)

data class RideOrder(
    val id: Long,
    val passengerId: Long,
    val driverId: Long?,
    val fromLat: Double,
    val fromLon: Double,
    val fromAddress: String,
    val toLat: Double,
    val toLon: Double,
    val toAddress: String,
    val priceRub: Int,
    val agreedPriceRub: Int?,
    val displayPriceRub: Int,
    val status: String,
    val driverEtaMinutes: Int?,
    val passengerExiting: Boolean,
    val passengerRating: Int?,
    val driverRating: Int?,
    val createdAt: String?,
    val passenger: RideUserBrief?,
    val driver: RideUserBrief?
)
