package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.driveeup.mobile.R
import ru.driveeup.mobile.domain.User

private val BrandGreen = Color(0xFF96EA28)

private val ReyvoHeroBottomRadius = 22.dp
private val TierIconDp = 36.dp
private val BrandCornerRadius = 16.dp

private const val OpenAllGrayAlpha = 0.55f

@Composable
fun DriveUpScreen(
    user: User,
    onOpenMenu: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenBattlePass: () -> Unit,
    onNotifications: () -> Unit = {}
) {
    val displayName = user.firstName?.takeIf { it.isNotBlank() }
        ?: user.email.substringBefore("@").ifBlank { "друг" }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .heightIn(min = 44.dp)
                .padding(horizontal = 0.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Меню",
                    tint = BrandGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_logo_driveup),
                contentDescription = "DriveUP",
                modifier = Modifier
                    .height(18.dp)
                    .padding(start = 2.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onNotifications,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_notify),
                    contentDescription = "Уведомления",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-2).dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = ReyvoHeroBottomRadius,
                            bottomEnd = ReyvoHeroBottomRadius
                        )
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.reyvo_hello),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter
                )
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Привет, $displayName!",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "На твоем счету: ",
                                color = Color(0xFF1D2A08),
                                fontSize = 15.sp
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_coin),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " ${user.driveCoin} ",
                                color = Color(0xFF1D2A08),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "койнов.",
                                color = Color(0xFF1D2A08),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DriveUpLoyaltySection(user = user)
                Text(
                    text = "К следующей поездке применится:",
                    color = Color(0xFF1D2A08),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                DriveUpNextRideBenefitCard(tier = loyaltyTierFromRides(user.ridesCount))

                DriveUpSpendCoinsSection(
                    onOpenAll = { /* TODO: later user will describe */ }
                )

                DriveUpTasksSection(
                    onOpenAll = { /* TODO: later user will describe */ }
                )

                Button(
                    onClick = onOpenGames,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color(0xFF1D2A08)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Игры", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onOpenBattlePass,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0F0F0),
                        contentColor = Color(0xFF1D2A08)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Батл-Пасс", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DriveUpLoyaltySection(user: User) {
    val rides = user.ridesCount
    val tier = loyaltyTierFromRides(rides)
    val remaining = ridesUntilNextTier(rides)
    val pBronzeSilver = progressBronzeToSilverBar(rides)
    val pSilverGold = progressSilverToGoldBar(rides)

    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.loality_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentScale = ContentScale.FillWidth
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Поздравляем! Твой уровень:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = loyaltyTierLabel(tier),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Image(
                    painter = painterResource(loyaltyTierIconRes(tier)),
                    contentDescription = loyaltyTierLabel(tier),
                    modifier = Modifier
                        .size(56.dp)
                        .padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (remaining != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.reyvo_run),
                        contentDescription = null,
                        modifier = Modifier.height(52.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = ridesUntilNextLevelPhrase(remaining),
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    )
                }
            } else {
                Text(
                    text = "Вы на максимальном уровне лояльности!",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            LoyaltyTierIconsWithProgress(
                pBronzeSilver = pBronzeSilver,
                pSilverGold = pSilverGold
            )
        }
    }
}

/** Полосы прогресса на одной линии с иконками; линия проходит под иконками (иконки сверху). */
@Composable
private fun LoyaltyTierIconsWithProgress(
    pBronzeSilver: Float,
    pSilverGold: Float
) {
    val icon = TierIconDp
    val halfIcon = icon / 2
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = halfIcon)
                .zIndex(0f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { pBronzeSilver },
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandGreen,
                trackColor = Color(0x66FFFFFF)
            )
            LinearProgressIndicator(
                progress = { pSilverGold },
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandGreen,
                trackColor = Color(0x66FFFFFF)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.loyalty_bronze),
                contentDescription = "Бронза",
                modifier = Modifier.size(icon)
            )
            Image(
                painter = painterResource(R.drawable.loyalty_silver),
                contentDescription = "Серебро",
                modifier = Modifier.size(icon)
            )
            Image(
                painter = painterResource(R.drawable.loyalty_gold),
                contentDescription = "Золото",
                modifier = Modifier.size(icon)
            )
        }
    }
}

@Composable
private fun DriveUpNextRideBenefitCard(tier: LoyaltyTier) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.loality_bg2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentScale = ContentScale.FillWidth
        )
        Text(
            text = loyaltyTierRideBenefit(tier),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .align(Alignment.Center),
            color = BrandGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

private data class SpendCoinItem(
    val id: String,
    val iconRes: Int,
    val description: String,
    val priceCoins: Long
)

private data class LoyaltyTaskItem(
    val id: String,
    val rewardCoins: Long,
    val title: String,
    val description: String
)

/**
 * Заглушки до подключения админ-API:
 * - администратор будет задавать список товаров/заданий
 * - в UI должен подставиться их icon/описания/стоимость/награды
 */
private val dummySpendItems = listOf(
    SpendCoinItem(
        id = "s1",
        iconRes = R.drawable.ic_coin,
        description = "Промо-предмет для твоего прогресса",
        priceCoins = 250
    ),
    SpendCoinItem(
        id = "s2",
        iconRes = R.drawable.ic_coin,
        description = "Дополнительный буст для поездок",
        priceCoins = 500
    )
)

private val dummyTasks = listOf(
    LoyaltyTaskItem(
        id = "t1",
        rewardCoins = 120,
        title = "Прокатиcь 1 раз",
        description = "Сделай одну поездку в DriveUP."
    ),
    LoyaltyTaskItem(
        id = "t2",
        rewardCoins = 300,
        title = "Прокатиcь 3 раза",
        description = "Собери серию и получи награду."
    ),
    LoyaltyTaskItem(
        id = "t3",
        rewardCoins = 650,
        title = "Прокатиcь 5 раз",
        description = "Заверши серию поездок и забери монеты."
    )
)

@Composable
private fun DriveUpHeaderRow(
    title: String,
    onOpenAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFF1D2A08),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Открыть все",
            color = Color.Gray.copy(alpha = OpenAllGrayAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onOpenAll)
        )
    }
}

@Composable
private fun DriveUpSpendCoinsSection(
    onOpenAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DriveUpHeaderRow(title = "Потратить ДрайвКойны", onOpenAll = onOpenAll)

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
        ) {
            items(dummySpendItems) { item ->
                SpendCoinCard(item = item)
            }
        }
    }
}

@Composable
private fun SpendCoinCard(item: SpendCoinItem) {
    val cardHeightDp = 300.dp
    Card(
        modifier = Modifier
            .width(230.dp)
            .heightIn(min = cardHeightDp),
        shape = RoundedCornerShape(BrandCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeightDp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(90.dp)
                )
            }

            Text(
                text = item.description,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.priceCoins.toString(),
                    color = Color(0xFF1D2A08),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { /* TODO: buy action */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color(0xFF1D2A08)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Купить", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DriveUpTasksSection(
    onOpenAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DriveUpHeaderRow(title = "Задания", onOpenAll = onOpenAll)

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
        ) {
            items(dummyTasks) { task ->
                LoyaltyTaskCard(task = task)
            }
        }
    }
}

@Composable
private fun LoyaltyTaskCard(task: LoyaltyTaskItem) {
    Card(
        modifier = Modifier
            .width(185.dp)
            .height(200.dp),
        shape = RoundedCornerShape(BrandCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(2.dp, BrandGreen)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.zad_bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Награда:",
                        color = Color(0xFF1D2A08),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_coin),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = task.rewardCoins.toString(),
                        color = Color(0xFF1D2A08),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = task.title,
                    color = Color(0xFF1D2A08),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = task.description,
                    color = Color.Gray.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 4
                )
            }
        }
    }
}
