package ru.driveeup.mobile.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import okhttp3.Headers
import ru.driveeup.mobile.R
import ru.driveeup.mobile.data.AchievementIconUrl

private val IconBrandGreen = Color(0xFF96EA28)

private const val ACH_LOG = "DriveUpAchievements"

/**
 * Отображение иконки достижения: нормализация URL, заголовки как у API, явные состояния загрузки / ошибки.
 */
@Composable
fun AchievementIconImage(
    iconUrl: String?,
    token: String,
    achievementId: Long,
    modifier: Modifier = Modifier,
    apiBase: String = AchievementIconUrl.DEFAULT_API_BASE,
) {
    val context = LocalContext.current
    val validated = remember(iconUrl, apiBase) {
        AchievementIconUrl.normalize(iconUrl, apiBase)
    }

    if (validated == null) {
        Box(modifier.then(Modifier.size(52.dp)))
        return
    }

    val request = remember(validated, token) {
        ImageRequest.Builder(context)
            .data(validated)
            .headers(achievementIconHeaders(token, validated))
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = request)
    val errorPainter = painterResource(R.drawable.ic_coin)

    LaunchedEffect(painter.state, validated, achievementId) {
        val st = painter.state
        if (st is AsyncImagePainter.State.Error) {
            Log.e(
                ACH_LOG,
                "Coil ERROR id=$achievementId url=$validated cause=${st.result.throwable.message}",
                st.result.throwable
            )
        }
    }

    Box(modifier.then(Modifier.size(52.dp)), contentAlignment = Alignment.Center) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = IconBrandGreen,
                    strokeWidth = 2.dp
                )
            is AsyncImagePainter.State.Error ->
                Image(
                    painter = errorPainter,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    contentScale = ContentScale.Fit
                )
            is AsyncImagePainter.State.Success ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    contentScale = ContentScale.Fit
                )
            is AsyncImagePainter.State.Empty ->
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = IconBrandGreen,
                    strokeWidth = 2.dp
                )
        }
    }
}

private fun achievementIconHeaders(token: String, imageUrl: String): Headers {
    val b =
        Headers.Builder()
            .add(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 DriveUP/1.0"
            )
            .add("Accept", "image/avif,image/webp,image/apng,image/png,image/jpeg,image/svg+xml,image/*,*/*;q=0.8")
    if (token.isNotBlank()) {
        b.add("Authorization", "Bearer $token")
    }
    try {
        val uri = java.net.URI(imageUrl)
        val scheme = uri.scheme ?: "https"
        val host = uri.host
        if (host != null) {
            val port = uri.port
            val origin =
                if (port > 0 && port != 80 && port != 443) {
                    "$scheme://$host:$port"
                } else {
                    "$scheme://$host"
                }
            b.add("Referer", "$origin/")
        }
    } catch (_: Exception) {
        b.add("Referer", "https://driveeup.ru/")
    }
    return b.build()
}
