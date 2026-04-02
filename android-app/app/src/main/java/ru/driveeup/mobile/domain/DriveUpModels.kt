package ru.driveeup.mobile.domain

data class DriveUpStoreItem(
    val id: Long,
    val name: String,
    val iconUrl: String?,
    val shortDescription: String,
    val allowedTier: String,
    val description: String,
    val usageTerms: String,
    val validityText: String,
    val priceDriveCoin: Long,
    val isAvailableForCurrentTier: Boolean,
    val sortOrder: Int
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
    val driveCoin: Long,
    val ridesCount: Long,
    val nextRideBenefitForTier: String,
    val loyaltyLevelDescriptions: Map<String, String>,
    val storeItems: List<DriveUpStoreItem>,
    val tasks: List<DriveUpTaskItem>
)

