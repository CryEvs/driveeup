package ru.driveeup.mobile.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

private const val SITE_BASE = "https://driveeup.ru"
private val CROSS_ROAD_URL = "$SITE_BASE/games/cross-road?embed=1"

/** Как в frontend/src/crossy/crossyCooldown.js — то же хранилище в WebView */
private const val LS_COOLDOWN_KEY = "driveeup_cross_road_cooldown_until"

private const val PREFS = "driveeup_games"
private const val KEY_COOLDOWN_UNTIL = "cross_road_cooldown_until"
private const val COOLDOWN_MS = 60_000L

private fun cooldownRemainingMs(ctx: Context): Long {
    val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val until = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
    return max(0L, until - System.currentTimeMillis())
}

private fun startCooldownNative(ctx: Context) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong(KEY_COOLDOWN_UNTIL, System.currentTimeMillis() + COOLDOWN_MS)
        .apply()
}

/** Как formatCooldown на сайте (ceil секунд) */
private fun formatCooldownRu(ms: Long): String {
    val s = ceil(ms / 1000.0).toInt().coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return if (m > 0) "$m мин $r с" else "$r с"
}

private fun parseEvaluateJavascriptLong(raw: String?): Long {
    if (raw.isNullOrBlank() || raw == "null") return 0L
    val t = raw.trim().trim('"').filter { it.isDigit() || it == '-' }
    return t.toLongOrNull() ?: 0L
}

/**
 * Список игр и WebView на те же URL и тексты, что на driveeup.ru (см. GamesPage.jsx).
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun GamesScreen() {
    val context = LocalContext.current
    var showGame by remember { mutableStateOf(false) }
    var cooldownMs by remember { mutableStateOf(cooldownRemainingMs(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            cooldownMs = cooldownRemainingMs(context)
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Игры", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Бесконечная 3D-аркада: трава, дороги с машинами, ж/д. Зарабатывай DriveeCoin за пройденные полосы.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Перебеги дорогу", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Только вперёд. Старт с одной травы; не больше одной зелёной подряд; подряд полос с машинами 1\u20134, после " +
                        "серии дорог — трава; ж/д и дороги чередуются. На дороге машины в одном направлении. " +
                        "После аварии получи DriveeCoin на баланс.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cooldownMs > 0) {
                    Text(
                        "Следующая игра через ${formatCooldownRu(cooldownMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showGame = true },
                    enabled = cooldownMs <= 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF96EA28),
                        contentColor = Color(0xFF1D2A08),
                        disabledContainerColor = Color(0xFF96EA28).copy(alpha = 0.45f),
                        disabledContentColor = Color(0xFF1D2A08).copy(alpha = 0.7f)
                    )
                ) {
                    Text("Играть")
                }
            }
        }
    }

    if (showGame) {
        Dialog(
            onDismissRequest = { showGame = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                scrimColor = Color.Transparent
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val webH = maxHeight * 0.67f
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(webH),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 3.dp
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    view?.evaluateJavascript(
                                                        "(function(){try{var v=localStorage.getItem('$LS_COOLDOWN_KEY');" +
                                                            "return v?parseInt(v,10):0;}catch(e){return 0;}})()"
                                                    ) { result ->
                                                        val until = parseEvaluateJavascriptLong(result)
                                                        if (until > System.currentTimeMillis()) {
                                                            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                                                .edit()
                                                                .putLong(KEY_COOLDOWN_UNTIL, until)
                                                                .apply()
                                                        }
                                                    }
                                                }
                                            }
                                            addJavascriptInterface(
                                                object {
                                                    @JavascriptInterface
                                                    fun notifyCooldown() {
                                                        startCooldownNative(ctx)
                                                    }
                                                },
                                                "AndroidCrossy"
                                            )
                                            loadUrl(CROSS_ROAD_URL)
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { showGame = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(40.dp)
                                        .background(
                                            Color(0f, 0f, 0f, 0.45f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Закрыть",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
