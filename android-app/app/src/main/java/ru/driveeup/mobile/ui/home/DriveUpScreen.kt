package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
                .fillMaxWidth(),
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
                        .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
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
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
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
                .align(Alignment.CenterVertically)
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
                .align(Alignment.CenterVertically)
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
