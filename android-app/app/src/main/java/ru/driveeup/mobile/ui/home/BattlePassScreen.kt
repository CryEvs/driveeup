package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun BattlePassScreen(token: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var seasonName by remember { mutableStateOf("Батл-Пасс") }
    var progressPct by remember { mutableStateOf(0f) }
    var levels by remember { mutableStateOf(listOf<JSONObject>()) }

    LaunchedEffect(token) {
        loading = true
        error = null
        if (token.isBlank()) {
            error = "Ошибка авторизации: пустой токен"
            loading = false
            return@LaunchedEffect
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val conn = URL("https://driveeup.ru/api/battle-pass/current").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IllegalStateException(
                        if (code == 401) "Ошибка авторизации (401). Войдите в аккаунт заново."
                        else "Ошибка загрузки батл-пасса ($code)"
                    )
                }
                val json = JSONObject(body)
                seasonName = json.optJSONObject("season")?.optString("name").orEmpty().ifBlank { "Батл-Пасс" }
                val seasonCoin = json.optDouble("seasonDriveCoin", 0.0)
                val arr = json.optJSONArray("levels")
                val parsed = mutableListOf<JSONObject>()
                var maxReq = 1.0
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        parsed.add(item)
                        maxReq = maxOf(maxReq, item.optDouble("requiredDriveCoin", 0.0))
                    }
                }
                levels = parsed
                progressPct = (seasonCoin / maxReq).coerceIn(0.0, 1.0).toFloat()
            }
        }.onFailure {
            error = it.message ?: "Ошибка загрузки"
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                shape = CircleShape,
                color = Color(0xFFF0F0F0)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color(0xFF6C6C6C),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(seasonName, modifier = Modifier.padding(bottom = 8.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(220.dp)
                    .background(Color(0xFFE6EEE0), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((220 * progressPct).dp)
                        .background(Color(0xFF74C515), RoundedCornerShape(999.dp))
                )
            }
            if (loading) Text("Загрузка...")
            error?.let { Text(it, color = Color(0xFFC93232)) }
        }
        items(levels) { lvl ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(10.dp)) {
                    val iconUrl = lvl.optString("iconUrl")
                    if (iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("Уровень ${lvl.optInt("levelNumber")}", color = Color(0xFF7E8580))
                        Text("${lvl.optInt("requiredDriveCoin")} DriveCoin", color = Color(0xFF7E8580))
                    }
                }
            }
        }
    }
}
