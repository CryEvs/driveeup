package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.driveeup.mobile.R
import ru.driveeup.mobile.data.DriveUpRepository

private val BrandGreen = Color(0xFF96EA28)
private val DriveUpDarkBg = Color(0xFF171918)
private val BrandCornerRadius = 16.dp

private data class BpLevel(
    val id: Long,
    val levelNumber: Int,
    val levelTitle: String?,
    val requiredDriveCoin: Int,
    val iconUrl: String?,
    val description: String?,
    val giftType: String?,
    val giftDriveCoin: Int?,
    val giftText: String?,
    val giftPromoCode: String?,
    val giftHidden: Boolean,
    val giftClaimed: Boolean,
)

private fun parseBpLevel(o: JSONObject) = BpLevel(
    id = o.optLong("id"),
    levelNumber = o.optInt("levelNumber"),
    levelTitle = o.optString("levelTitle").takeUnless { it.isBlank() || it == "null" },
    requiredDriveCoin = o.optInt("requiredDriveCoin"),
    iconUrl = o.optString("iconUrl").takeUnless { it.isBlank() || it == "null" },
    description = o.optString("description").takeUnless { it.isBlank() || it == "null" },
    giftType = o.optString("giftType").takeUnless { it.isBlank() || it == "null" },
    giftDriveCoin = if (o.isNull("giftDriveCoin")) null else o.optInt("giftDriveCoin"),
    giftText = o.optString("giftText").takeUnless { it.isBlank() || it == "null" },
    giftPromoCode = o.optString("giftPromoCode").takeUnless { it.isBlank() || it == "null" },
    giftHidden = o.optBoolean("giftHidden", true),
    giftClaimed = o.optBoolean("giftClaimed", false),
)

@Composable
fun BattlePassScreen(
    token: String,
    onBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit = {},
) {
    val repo = remember { DriveUpRepository() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var seasonName by remember { mutableStateOf("Драйв-Пасс") }
    var seasonDriveCoin by remember { mutableStateOf(0.0) }
    var levels by remember { mutableStateOf<List<BpLevel>>(emptyList()) }
    var claimingId by remember { mutableStateOf<Long?>(null) }
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(token, reloadTick) {
        loading = true
        error = null
        if (token.isBlank()) {
            error = "Ошибка авторизации: пустой токен"
            loading = false
            return@LaunchedEffect
        }
        runCatching {
            val json = repo.battlePassCurrent(token)
            val season = json.optJSONObject("season")
            seasonName = season?.optString("name").orEmpty().ifBlank { "Драйв-Пасс" }
            if (season == null) {
                levels = emptyList()
                seasonDriveCoin = 0.0
            } else {
                seasonDriveCoin = json.optDouble("seasonDriveCoin", 0.0)
                val arr = json.optJSONArray("levels")
                val parsed = mutableListOf<BpLevel>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        parsed.add(parseBpLevel(arr.getJSONObject(i)))
                    }
                }
                levels = parsed
            }
        }.onFailure {
            error = it.message ?: "Ошибка загрузки"
        }
        loading = false
    }

    val maxTarget = remember(levels, seasonDriveCoin) {
        val m = levels.maxOfOrNull { it.requiredDriveCoin }?.toDouble() ?: 1.0
        kotlin.math.max(m, seasonDriveCoin).coerceAtLeast(1.0)
    }
    val progress = (seasonDriveCoin / maxTarget).toFloat().coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(DriveUpDarkBg)) {
        DriveUpTopBar(onBack = onMenuBack, onNotifications = onNotifications, dark = true)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Драйв-Пасс", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(seasonName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Прогресс сезона", color = Color(0xFF1D2A08), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                                Text(
                                    " ${seasonDriveCoin.toInt()} / ${maxTarget.toInt()}",
                                    color = Color(0xFF1D2A08),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = BrandGreen,
                                trackColor = Color(0xFFE0E0E0),
                            )
                        }
                    }
                }
            }

            if (loading) {
                item { CircularProgressIndicator(color = BrandGreen) }
            }
            error?.let { err ->
                item { Text(err, color = Color(0xFFFF8A80), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            }

            if (!loading && levels.isEmpty() && error == null) {
                item {
                    Text(
                        "Нет активного сезона Драйв-Пасса",
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            val rows = levels.chunked(2)
            items(rows) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { lvl ->
                        DrivePassLevelCard(
                            level = lvl,
                            claiming = claimingId == lvl.id,
                            onClaim = {
                                if (lvl.giftClaimed || lvl.giftHidden || lvl.giftType.isNullOrBlank()) return@DrivePassLevelCard
                                claimingId = lvl.id
                                scope.launch {
                                    runCatching { repo.claimBattlePassGift(token, lvl.id) }
                                    claimingId = null
                                    reloadTick++
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DrivePassLevelCard(
    level: BpLevel,
    claiming: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText = level.levelTitle?.takeIf { it.isNotBlank() } ?: "Уровень ${level.levelNumber}"
    val descText = level.description?.takeIf { it.isNotBlank() } ?: ""

    Surface(
        modifier = modifier.width(185.dp).height(210.dp),
        shape = RoundedCornerShape(BrandCornerRadius),
        border = BorderStroke(1.dp, BrandGreen),
        shadowElevation = 6.dp,
        color = Color.White
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painterResource(R.drawable.zad_bg),
                null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Награда:", color = Color(0xFF1D2A08), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    when {
                        level.giftHidden -> Text("скрыта", color = Color.Gray, fontSize = 11.sp)
                        level.giftClaimed -> Text("получена", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        level.giftType == "DRIVECOIN" -> {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                (level.giftDriveCoin ?: 0).toString(),
                                color = Color(0xFF1D2A08),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        level.giftType == "PROMO_CODE" -> Text("промокод", color = Color(0xFF1D2A08), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        level.giftType == "TEXT" -> Text("текст", color = Color(0xFF1D2A08), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        else -> Text("—", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                if (level.iconUrl != null) {
                    Spacer(Modifier.height(6.dp))
                    AsyncImage(
                        model = level.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(titleText, color = Color(0xFF1D2A08), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(descText, color = Color.Gray.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(12.dp))
                    Text(
                        " ${level.requiredDriveCoin} за сезон",
                        color = Color(0xFF7E8580),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                val canClaim = !level.giftHidden && !level.giftClaimed && !level.giftType.isNullOrBlank()
                if (canClaim) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onClaim,
                        enabled = !claiming,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color(0xFF1D2A08)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (claiming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF1D2A08),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Забрать", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
