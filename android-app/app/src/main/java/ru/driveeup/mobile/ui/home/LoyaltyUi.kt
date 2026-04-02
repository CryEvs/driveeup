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
    LoyaltyTier.BRONZE -> R.drawable.loyalty_bronze
    LoyaltyTier.SILVER -> R.drawable.loyalty_silver
    LoyaltyTier.GOLD -> R.drawable.loyalty_gold
}

fun loyaltyTierLabel(tier: LoyaltyTier): String = when (tier) {
    LoyaltyTier.BRONZE -> "Бронза"
    LoyaltyTier.SILVER -> "Серебро"
    LoyaltyTier.GOLD -> "Золото"
}

/**
 * Пороги по **количеству поездок** для уровней на экране DriveUP.
 * Значения задаёт администратор; позже можно подставить из API.
 */
object LoyaltyRidesThresholds {
    const val RIDES_FOR_SILVER: Long = 15L
    const val RIDES_FOR_GOLD: Long = 50L
}

/** Уровень по числу поездок (экран лояльности DriveUP). */
fun loyaltyTierFromRides(ridesCount: Long): LoyaltyTier {
    return when {
        ridesCount >= LoyaltyRidesThresholds.RIDES_FOR_GOLD -> LoyaltyTier.GOLD
        ridesCount >= LoyaltyRidesThresholds.RIDES_FOR_SILVER -> LoyaltyTier.SILVER
        else -> LoyaltyTier.BRONZE
    }
}

/** Сколько поездок осталось до следующего уровня; `null` — уже максимум. */
fun ridesUntilNextTier(ridesCount: Long): Long? {
    return when {
        ridesCount < LoyaltyRidesThresholds.RIDES_FOR_SILVER ->
            LoyaltyRidesThresholds.RIDES_FOR_SILVER - ridesCount
        ridesCount < LoyaltyRidesThresholds.RIDES_FOR_GOLD ->
            LoyaltyRidesThresholds.RIDES_FOR_GOLD - ridesCount
        else -> null
    }
}

fun progressBronzeToSilverBar(ridesCount: Long): Float {
    val cap = LoyaltyRidesThresholds.RIDES_FOR_SILVER.coerceAtLeast(1L)
    return (ridesCount.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
}

fun progressSilverToGoldBar(ridesCount: Long): Float {
    val from = LoyaltyRidesThresholds.RIDES_FOR_SILVER
    val to = LoyaltyRidesThresholds.RIDES_FOR_GOLD
    if (to <= from) return 1f
    return when {
        ridesCount <= from -> 0f
        ridesCount >= to -> 1f
        else -> ((ridesCount - from).toFloat() / (to - from).toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Краткое преимущество текущего статуса (тексты можно заменить данными с сервера).
 */
fun loyaltyTierRideBenefit(tier: LoyaltyTier): String = when (tier) {
    LoyaltyTier.BRONZE -> "Базовые бонусы программы лояльности DriveUP"
    LoyaltyTier.SILVER -> "Расширенные бонусы и приоритет в программе"
    LoyaltyTier.GOLD -> "Максимальные привилегии и приоритет DriveUP"
}

/** Имя с числом: «Осталось N поездка/поездки/поездок …». */
private fun ridesNounAfterNumber(n: Long): String {
    val nn = n.toInt().coerceAtLeast(0)
    val mod100 = nn % 100
    val mod10 = nn % 10
    return when {
        mod100 in 11..19 -> "поездок"
        mod10 == 1 -> "поездка"
        mod10 in 2..4 -> "поездки"
        else -> "поездок"
    }
}

/** Фраза для блока с reyvo_run: «Осталось N … до повышения уровня». */
fun ridesUntilNextLevelPhrase(remaining: Long): String {
    if (remaining <= 0L) return ""
    val nn = remaining.toInt()
    return "Осталось $nn ${ridesNounAfterNumber(remaining)} до повышения уровня"
}
