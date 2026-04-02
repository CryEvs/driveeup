package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.driveeup.mobile.R
import ru.driveeup.mobile.data.DriveUpRepository
import ru.driveeup.mobile.domain.DriveUpContent
import ru.driveeup.mobile.domain.DriveUpStoreItem
import ru.driveeup.mobile.domain.DriveUpTaskItem
import ru.driveeup.mobile.domain.User

private val BrandGreen = Color(0xFF96EA28)
private val DriveUpDarkBg = Color(0xFF171918)
private val ReyvoHeroBottomRadius = 22.dp
private val BrandCornerRadius = 16.dp

@Composable
fun DriveUpScreen(
    user: User,
    token: String,
    onOpenMenu: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenBattlePass: () -> Unit,
    onOpenStoreAll: () -> Unit,
    onOpenTasksAll: () -> Unit,
    onOpenLoyaltyLevels: () -> Unit,
    onNotifications: () -> Unit = {}
) {
    val repo = remember { DriveUpRepository() }
    var content by remember { mutableStateOf<DriveUpContent?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<DriveUpStoreItem?>(null) }
    val displayName = user.firstName.takeIf { it.isNotBlank() } ?: user.email.substringBefore("@").ifBlank { "друг" }

    LaunchedEffect(token) {
        loading = true
        content = runCatching { repo.content(token) }.getOrNull()
        loading = false
    }

    val currentCoin = content?.driveCoin ?: user.driveCoin
    val items = content?.storeItems ?: emptyList()
    val tasks = content?.tasks ?: emptyList()
    val rides = content?.ridesCount ?: user.ridesCount
    val tier = loyaltyTierFromRides(rides)
    val nextRideBenefit = content?.nextRideBenefitForTier?.takeIf { it.isNotBlank() } ?: loyaltyTierRideBenefit(tier)

    Column(Modifier.fillMaxSize().background(Color.White)) {
        DriveUpTopBar(onBack = onOpenMenu, onNotifications = onNotifications)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            HeroSection(displayName = displayName, driveCoin = currentCoin)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.White).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.fillMaxWidth().clickable(onClick = onOpenLoyaltyLevels)) {
                    DriveUpLoyaltySection(tier = tier, ridesCount = rides)
                }
                Text("К следующей поездке применится:", color = Color(0xFF1D2A08), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                NextRideBenefitCard(nextRideBenefit)

                SectionHeader(title = "Потратить ДрайвКойны", onOpenAll = onOpenStoreAll)
                if (loading) {
                    CircularProgressIndicator(color = BrandGreen)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                        items(items.take(8)) { item -> StoreItemCard(item = item, onClick = { selectedItem = item }) }
                    }
                }

                SectionHeader(title = "Задания", onOpenAll = onOpenTasksAll)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    items(tasks.take(8)) { task -> TaskCard(task) }
                }

                Button(onClick = onOpenGames, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color(0xFF1D2A08)),
                    shape = RoundedCornerShape(12.dp)) { Text("Игры", fontWeight = FontWeight.SemiBold) }
                Button(onClick = onOpenBattlePass, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0), contentColor = Color(0xFF1D2A08)),
                    shape = RoundedCornerShape(12.dp)) { Text("Батл-Пасс", fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (selectedItem != null) {
        StoreItemDialog(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onBuy = { selectedItem = null /* TODO purchase API */ }
        )
    }
}

@Composable
fun DriveUpLoyaltyLevelsScreen(
    user: User,
    token: String,
    onBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit = {}
) {
    val repo = remember { DriveUpRepository() }
    var content by remember { mutableStateOf<DriveUpContent?>(null) }
    var selectedTier by remember { mutableStateOf("BRONZE") }
    val rides = content?.ridesCount ?: user.ridesCount
    val currentTier = loyaltyTierFromRides(rides)
    val descriptions = content?.loyaltyLevelDescriptions ?: mapOf(
        "BRONZE" to "Бронзовый уровень: стартовые привилегии и базовые преимущества.",
        "SILVER" to "Серебряный уровень: больше бонусов и улучшенные условия.",
        "GOLD" to "Золотой уровень: максимальные привилегии и лучшие условия сервиса."
    )

    LaunchedEffect(token) {
        content = runCatching { repo.content(token) }.getOrNull()
        selectedTier = currentTier.name
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DriveUpDarkBg)
        ) {
            DriveUpTopBar(onBack = onMenuBack, onNotifications = onNotifications, dark = true)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                }
                Text("Система уровней", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                DriveUpLoyaltySection(tier = currentTier, ridesCount = rides)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                LoyaltyTab("Бронза", "#383C47", selectedTier == "BRONZE", modifier = Modifier.weight(1f)) { selectedTier = "BRONZE" }
                LoyaltyTab("Серебро", "#97EA28", selectedTier == "SILVER", modifier = Modifier.weight(1f)) { selectedTier = "SILVER" }
                LoyaltyTab("Золото", "#F24B16", selectedTier == "GOLD", modifier = Modifier.weight(1f)) { selectedTier = "GOLD" }
            }
            Spacer(Modifier.height(18.dp))
            Text("Описание", color = Color(0xFF1D2A08), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(descriptions[selectedTier].orEmpty(), color = Color(0xFF1D2A08), fontSize = 15.sp)
        }
    }
}

@Composable
private fun LoyaltyTab(
    title: String,
    colorHex: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = color,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) color else Color.Transparent)
        )
    }
}

@Composable
fun DriveUpStoreAllScreen(
    user: User,
    token: String,
    onBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit = {}
) {
    val repo = remember { DriveUpRepository() }
    var items by remember { mutableStateOf<List<DriveUpStoreItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<DriveUpStoreItem?>(null) }
    var coin by remember { mutableStateOf(user.driveCoin) }

    LaunchedEffect(token) {
        loading = true
        runCatching {
            val c = repo.content(token)
            items = c.storeItems
            coin = c.driveCoin
        }
        loading = false
    }

    DarkListBase(
        driveCoin = coin,
        title = "Потратить ДрайвКойны",
        onTitleBack = onBack,
        onMenuBack = onMenuBack,
        onNotifications = onNotifications
    ) {
        if (loading) {
            item { CircularProgressIndicator(color = BrandGreen) }
        } else {
            val rows = items.chunked(2)
            items(rows) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoreItemCard(row[0], onClick = { selectedItem = row[0] }, modifier = Modifier.weight(1f))
                    if (row.size > 1) {
                        StoreItemCard(row[1], onClick = { selectedItem = row[1] }, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        StoreItemDialog(item = selectedItem!!, onDismiss = { selectedItem = null }, onBuy = { selectedItem = null })
    }
}

@Composable
fun DriveUpTasksAllScreen(
    user: User,
    token: String,
    onBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit = {}
) {
    val repo = remember { DriveUpRepository() }
    var tasks by remember { mutableStateOf<List<DriveUpTaskItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var coin by remember { mutableStateOf(user.driveCoin) }
    LaunchedEffect(token) {
        loading = true
        runCatching {
            val c = repo.content(token)
            tasks = c.tasks
            coin = c.driveCoin
        }
        loading = false
    }

    DarkListBase(
        driveCoin = coin,
        title = "Выполнить задания",
        onTitleBack = onBack,
        onMenuBack = onMenuBack,
        onNotifications = onNotifications
    ) {
        if (loading) {
            item { CircularProgressIndicator(color = BrandGreen) }
        } else {
            val rows = tasks.chunked(2)
            items(rows) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskCard(row[0], modifier = Modifier.weight(1f))
                    if (row.size > 1) {
                        TaskCard(row[1], modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkListBase(
    driveCoin: Long,
    title: String,
    onTitleBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(DriveUpDarkBg)) {
        DriveUpTopBar(onBack = onMenuBack, onNotifications = onNotifications, dark = true)
        Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("На твоем счету: ", color = Color(0xFF1D2A08))
                Image(painterResource(R.drawable.ic_coin), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" $driveCoin койнов.", color = Color(0xFF1D2A08), fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTitleBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            content = content
        )
    }
}

@Composable
private fun DriveUpTopBar(
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    dark: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (dark) DriveUpDarkBg else Color.Black),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(22.dp))
        }
        Image(painterResource(R.drawable.ic_logo_driveup), contentDescription = "DriveUP", modifier = Modifier.height(18.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNotifications, modifier = Modifier.size(40.dp)) {
            Image(painterResource(R.drawable.ic_notify), contentDescription = "Уведомления", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun HeroSection(displayName: String, driveCoin: Long) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = ReyvoHeroBottomRadius, bottomEnd = ReyvoHeroBottomRadius))) {
        Image(painterResource(R.drawable.reyvo_hello), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        Column(Modifier.matchParentSize().padding(start = 36.dp, end = 36.dp, top = 26.dp, bottom = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Привет, $displayName!", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("На твоем счету: ", color = Color(0xFF1D2A08), fontSize = 15.sp)
                    Image(painterResource(R.drawable.ic_coin), contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(" $driveCoin ", color = Color(0xFF1D2A08), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("койнов.", color = Color(0xFF1D2A08), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun DriveUpLoyaltySection(tier: LoyaltyTier, ridesCount: Long) {
    val remaining = ridesUntilNextTier(ridesCount)
    val p1 = progressBronzeToSilverBar(ridesCount)
    val p2 = progressSilverToGoldBar(ridesCount)
    Box(Modifier.fillMaxWidth()) {
        Image(painterResource(R.drawable.loality_bg), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Поздравляем! Твой уровень:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(loyaltyTierLabel(tier), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Image(painterResource(loyaltyTierIconRes(tier)), contentDescription = loyaltyTierLabel(tier), modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (remaining != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.reyvo_run), contentDescription = null, modifier = Modifier.height(52.dp))
                    Text(ridesUntilNextLevelPhrase(remaining), color = Color.White, modifier = Modifier.padding(start = 10.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.loyalty_bronze), null, modifier = Modifier.size(36.dp))
                LinearProgressIndicator(progress = { p1 }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(7.dp), color = BrandGreen, trackColor = Color(0x66FFFFFF))
                Image(painterResource(R.drawable.loyalty_silver), null, modifier = Modifier.size(36.dp))
                LinearProgressIndicator(progress = { p2 }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(7.dp), color = BrandGreen, trackColor = Color(0x66FFFFFF))
                Image(painterResource(R.drawable.loyalty_gold), null, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun NextRideBenefitCard(text: String) {
    Box(Modifier.fillMaxWidth()) {
        Image(painterResource(R.drawable.loality_bg2), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        Text(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), color = BrandGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionHeader(title: String, onOpenAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color(0xFF1D2A08), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("Открыть все", color = Color.Gray, modifier = Modifier.clickable(onClick = onOpenAll))
    }
}

@Composable
private fun StoreItemCard(item: DriveUpStoreItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isGold = item.allowedTier == "GOLD"
    val isSilver = item.allowedTier == "SILVER"
    val rankColor = when {
        isGold -> Color(0xFFF24B16)
        isSilver -> Color(0xFF171918)
        else -> Color(0xFF97EA28)
    }
    val rankTitleColor = when {
        isGold -> Color(0xFFF7F3E7)
        isSilver -> Color(0xFF97EA28)
        else -> Color(0xFF171918)
    }
    val rankSubtitle = when {
        isGold -> "Для золотого уровня"
        isSilver -> "Для серебрянного уровня"
        else -> "Для любого уровня"
    }
    Card(
        modifier = modifier.width(200.dp).height(300.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(BrandCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = BrandCornerRadius, topEnd = BrandCornerRadius))
                    .background(rankColor)
            ) {
                Image(
                    painter = painterResource(R.drawable.driveup_design_arrows),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = item.name,
                        color = rankTitleColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = rankSubtitle,
                        color = rankTitleColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(item.shortDescription.ifBlank { item.name }, color = Color.Gray, fontSize = 12.sp, maxLines = 3)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.priceDriveCoin.toString(),
                        color = Color(0xFF1D2A08),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color(0xFF1D2A08)), shape = RoundedCornerShape(12.dp)) {
                    Text("Купить", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: DriveUpTaskItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(185.dp).height(200.dp),
        shape = RoundedCornerShape(BrandCornerRadius),
        border = BorderStroke(1.dp, BrandGreen),
        shadowElevation = 6.dp,
        color = Color.White
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.zad_bg), null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Награда:", color = Color(0xFF1D2A08), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(task.rewardDriveCoin.toString(), color = Color(0xFF1D2A08), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Text(task.title, color = Color(0xFF1D2A08), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Text(task.description, color = Color.Gray.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 4)
            }
        }
    }
}

@Composable
private fun StoreItemDialog(
    item: DriveUpStoreItem,
    onDismiss: () -> Unit,
    onBuy: () -> Unit
) {
    val rankColor = when (item.allowedTier) {
        "GOLD" -> Color(0xFFF24B16)
        "SILVER" -> Color(0xFF171918)
        else -> Color(0xFF97EA28)
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 18.dp).clickable(enabled = false) {},
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(120.dp).background(rankColor)) {
                        Image(painterResource(R.drawable.driveup_design_arrows), null, modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp))
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Text(item.name, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(item.shortDescription, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Описание", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(item.description.ifBlank { "-" }, fontSize = 13.sp)
                        Text("Условия пользования", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(item.usageTerms.ifBlank { "-" }, fontSize = 13.sp)
                        Text("Срок действия", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(item.validityText.ifBlank { "-" }, fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(18.dp))
                            Text(" ${item.priceDriveCoin}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Button(onClick = onBuy, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color(0xFF1D2A08))) {
                                Text("Купить", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
