package ru.driveeup.mobile.domain

data class DriveUpStoreItem(
    val id: Long,
    val name: String,
    val iconUrl: String?,
    val shortDescription: String,
    val allowedTier: String,
    val itemType: String,
    val discountPercent: Int?,
    val description: String,
    val usageTerms: String,
    val validityText: String,
    val priceDriveCoin: Long,
    val isAvailableForCurrentTier: Boolean,
    val sortOrder: Int
)

data class DriveUpNotification(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: String?
)

data class DriveUpTaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val rewardDriveCoin: Long,
    val sortOrder: Int
)

data class DriveUpContent(
    val loyaltyTier: String,
    val driveCoin: Double,
    val ridesCount: Long,
    val nextRideBenefitForTier: String,
    val nextRideStoreItemName: String?,
    val loyaltyLevelDescriptions: Map<String, String>,
    val loyaltyRidesThresholds: Map<String, Long>,
    val storeItems: List<DriveUpStoreItem>,
    val tasks: List<DriveUpTaskItem>
)

