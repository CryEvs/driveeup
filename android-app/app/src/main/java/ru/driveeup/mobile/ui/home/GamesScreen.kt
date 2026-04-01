package ru.driveeup.mobile.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

private const val SITE_BASE = "https://driveeup.ru"
private val CROSS_ROAD_URL = "$SITE_BASE/games/cross-road?embed=1"

/** Как в frontend/src/crossy/crossyCooldown.js */
private const val LS_COOLDOWN_KEY = "driveeup_cross_road_cooldown_until"

private const val PREFS = "driveeup_games"
private const val KEY_COOLDOWN_UNTIL = "cross_road_cooldown_until"

/** Как в AppScaffold: тот же JWT для WebView → authContext (readStoredOrAndroidToken). */
private const val AUTH_PREFS = "driveeup_prefs"
private const val KEY_AUTH_TOKEN = "token"
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

/** Как formatCooldown на сайте */
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

/** Как страница «Игры» на driveeup.ru: одна карточка-превью, без описания под ней. */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun GamesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showGame by remember { mutableStateOf(false) }
    var cooldownMs by remember { mutableStateOf(cooldownRemainingMs(context)) }

    fun closeGameDialog() {
        startCooldownNative(context)
        showGame = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            cooldownMs = cooldownRemainingMs(context)
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.TopStart),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF0F0F0),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color(0xFF6C6C6C),
                modifier = Modifier.padding(10.dp)
            )
        }
        CrossRoadGameCard(
            cooldownMs = cooldownMs,
            onPlay = { if (cooldownMs <= 0) showGame = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 52.dp)
        )
    }

    if (showGame) {
        Dialog(
            onDismissRequest = { closeGameDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x8E0A1208))
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
                    val webH = maxHeight * 0.82f
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = webH),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A120A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                                                    val prefsUntil = ctx.getSharedPreferences(
                                                        PREFS,
                                                        Context.MODE_PRIVATE
                                                    ).getLong(KEY_COOLDOWN_UNTIL, 0L)
                                                    val authToken = ctx.getSharedPreferences(
                                                        AUTH_PREFS,
                                                        Context.MODE_PRIVATE
                                                    ).getString(KEY_AUTH_TOKEN, "").orEmpty()
                                                    view?.evaluateJavascript(
                                                        "(function(){try{" +
                                                            "var k='$LS_COOLDOWN_KEY';" +
                                                            "var tk=" + org.json.JSONObject.quote(authToken) + ";" +
                                                            "var p=" + prefsUntil + ";" +
                                                            "var ls=0;" +
                                                            "try{var x=localStorage.getItem(k);" +
                                                            "ls=x?parseInt(x,10):0;}catch(e){}" +
                                                            "if(tk){" +
                                                            "try{localStorage.setItem('driveeup_token',tk);}catch(e){}" +
                                                            "try{sessionStorage.setItem('driveeup_token',tk);}catch(e){}" +
                                                            "try{" +
                                                            "if(!sessionStorage.getItem('driveeup_token_synced')){" +
                                                            "sessionStorage.setItem('driveeup_token_synced','1');" +
                                                            "setTimeout(function(){location.reload();},0);" +
                                                            "}" +
                                                            "}catch(e){}" +
                                                            "}" +
                                                            "var u=Math.max(p,ls);" +
                                                            "var now=Date.now();" +
                                                            "if(u>now){localStorage.setItem(k,String(u));}" +
                                                            "return u;}catch(e){return 0;}})()"
                                                    ) { result ->
                                                        val u = parseEvaluateJavascriptLong(result)
                                                        if (u > System.currentTimeMillis()) {
                                                            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                                                .edit()
                                                                .putLong(KEY_COOLDOWN_UNTIL, u)
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
                                            addJavascriptInterface(
                                                object {
                                                    @JavascriptInterface
                                                    fun getToken(): String {
                                                        return ctx.getSharedPreferences(
                                                            AUTH_PREFS,
                                                            Context.MODE_PRIVATE
                                                        ).getString(KEY_AUTH_TOKEN, "").orEmpty()
                                                    }
                                                },
                                                "AndroidAuth"
                                            )
                                            loadUrl(CROSS_ROAD_URL)
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { closeGameDialog() },
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
}

@Composable
private fun CrossRoadGameCard(
    cooldownMs: Long,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onCooldown = cooldownMs > 0
    val previewBg = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color(0xFF1E3A1A),
            0.6f to Color(0xFF0D1A0D),
            1f to Color(0xFF2A5A22)
        )
    )
    val voxelBrush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to Color(0xFF5CB85C),
            0.22f to Color(0xFF5CB85C),
            0.22f to Color(0xFF444444),
            0.48f to Color(0xFF444444),
            0.48f to Color(0xFF2A2A2A),
            0.68f to Color(0xFF2A2A2A),
            0.68f to Color(0xFF555555),
            1f to Color(0xFF555555)
        )
    )
    val overlayBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color(0x8C0A1208),
            0.38f to Color(0x000A1208),
            0.55f to Color(0x000A1208),
            1f to Color(0xC70A1208)
        )
    )

    Column(modifier = modifier.widthIn(max = 400.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF4F7A3F), RoundedCornerShape(16.dp))
        ) {
            val padH = maxWidth * 0.06f
            val padV = maxHeight * 0.08f
            Box(Modifier.fillMaxSize().background(previewBg))
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = padH, vertical = padV)
                    .clip(RoundedCornerShape(6.dp))
                    .background(voxelBrush)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
                    .padding(horizontal = 14.dp, vertical = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Перебеги дорогу",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onCooldown) {
                        Text(
                            text = "Следующая игра через ${formatCooldownRu(cooldownMs)}",
                            color = Color(0xFFE8F5D0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Button(
                        onClick = onPlay,
                        enabled = !onCooldown,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 320.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF96EA28),
                            contentColor = Color(0xFF1D2A08),
                            disabledContainerColor = Color(0xFF96EA28).copy(alpha = 0.45f),
                            disabledContentColor = Color(0xFF1D2A08).copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Играть", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
