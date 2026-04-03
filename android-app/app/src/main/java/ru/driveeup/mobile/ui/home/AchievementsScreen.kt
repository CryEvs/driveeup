package ru.driveeup.mobile.ui.home

import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.driveeup.mobile.R
import ru.driveeup.mobile.data.DriveUpRepository
import ru.driveeup.mobile.domain.AchievementItem

private val BrandGreen = Color(0xFF96EA28)
private val DriveUpDarkBg = Color(0xFF171918)
private val BrandCornerRadius = 16.dp

/** Искать в Logcat по этому тегу (фильтр: `DriveUpAchievements`). */
private const val ACH_LOG = "DriveUpAchievements"

private fun logAch(msg: String, throwable: Throwable? = null) {
    // Log.w / Log.e видны при фильтре «Warning» и «Error»; Log.d/i часто отключены.
    if (throwable != null) {
        Log.e(ACH_LOG, msg, throwable)
    } else {
        Log.w(ACH_LOG, msg)
    }
    println("$ACH_LOG: $msg")
}

@Composable
fun AchievementsScreen(
    token: String,
    onBack: () -> Unit,
    onMenuBack: () -> Unit,
    onNotifications: () -> Unit = {},
) {
    val repo = remember { DriveUpRepository() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var achievementItems by remember { mutableStateOf<List<AchievementItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        logAch("экран Достижения открыт — если это не видно, смените уровень лога на Warning/Verbose")
    }

    LaunchedEffect(token) {
        loading = true
        error = null
        if (token.isBlank()) {
            error = "Ошибка авторизации"
            loading = false
            return@LaunchedEffect
        }
        runCatching {
            achievementItems = repo.achievementsList(token)
            logAch("API: загружено достижений=${achievementItems.size}")
            achievementItems.forEach { a ->
                logAch("API: id=${a.id} iconUrl=${a.iconUrl ?: "null"}")
            }
        }.onFailure {
            error = it.message ?: "Ошибка загрузки"
            logAch("API: ошибка списка: ${it.message}", it)
        }
        loading = false
    }

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
            Text("Достижения", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            if (loading) {
                item { CircularProgressIndicator(color = BrandGreen, modifier = Modifier.padding(24.dp)) }
            }
            error?.let { err ->
                item {
                    Text(err, color = Color(0xFFFF8A80), modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                }
            }
            if (!loading && achievementItems.isEmpty() && error == null) {
                item {
                    Text(
                        "Пока нет достижений",
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            val rows = achievementItems.chunked(2)
            items(rows) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { ach ->
                        AchievementCard(ach, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(185.dp).height(220.dp),
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
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 5.dp)
                ) {
                    AchievementIconImage(
                        iconUrl = item.iconUrl,
                        achievementId = item.id,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        item.title,
                        color = Color(0xFF1D2A08),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.description,
                        color = Color.Gray.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 6
                    )
                }
            }
        }
    }
}
