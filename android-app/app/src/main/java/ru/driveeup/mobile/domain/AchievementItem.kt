package ru.driveeup.mobile.domain

data class AchievementItem(
    val id: Long,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val awardType: String?,
    val ridesRequired: Int?,
)
