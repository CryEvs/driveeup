package ru.driveeup.mobile.ui.home

import androidx.annotation.DrawableRes
import ru.driveeup.mobile.R
import ru.driveeup.mobile.domain.User

enum class LoyaltyTier {
    BRONZE, SILVER, GOLD
}

fun loyaltyTier(user: User): LoyaltyTier {
    val c = user.totalDriveCoin
    return when {
        c >= 10_000 -> LoyaltyTier.GOLD
        c >= 2_500 -> LoyaltyTier.SILVER
        else -> LoyaltyTier.BRONZE
    }
}

@DrawableRes
fun loyaltyTierIconRes(tier: LoyaltyTier): Int = when (tier) {
    LoyaltyTier.BRONZE -> R.drawable.ic_tier_bronze
    LoyaltyTier.SILVER -> R.drawable.ic_tier_silver
    LoyaltyTier.GOLD -> R.drawable.ic_tier_gold
}

fun loyaltyTierLabel(tier: LoyaltyTier): String = when (tier) {
    LoyaltyTier.BRONZE -> "Бронза"
    LoyaltyTier.SILVER -> "Серебро"
    LoyaltyTier.GOLD -> "Золото"
}
