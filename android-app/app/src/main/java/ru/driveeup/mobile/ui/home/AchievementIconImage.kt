package ru.driveeup.mobile.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import ru.driveeup.mobile.R
import ru.driveeup.mobile.data.AchievementIconUrl

private val IconBrandGreen = Color(0xFF96EA28)

private const val ACH_LOG = "DriveUpAchievements"

/**
 * Иконка достижения: нормализация URL.
 *
 * Сначала пробуем `/storage/achievement-icons/…`, затем исходный URL из API — тот же файл на диске,
 * но на части конфигураций один путь с телефона даёт 404, другой — 200.
 *
 * Во время Loading/Empty нужно всё равно отрисовывать [Image] с [rememberAsyncImagePainter] —
 * иначе Coil не получает constraints и загрузка может не завершаться (бесконечный спиннер).
 */
@Composable
fun AchievementIconImage(
    iconUrl: String?,
    achievementId: Long,
    modifier: Modifier = Modifier,
    apiBase: String = AchievementIconUrl.DEFAULT_API_BASE,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val validated = remember(iconUrl, apiBase) {
        AchievementIconUrl.normalize(iconUrl, apiBase)
    }

    if (validated == null) {
        Box(modifier.then(Modifier.size(52.dp)))
        return
    }

    val candidates = remember(validated, apiBase) {
        AchievementIconUrl.loadCandidates(validated, apiBase)
    }
    var attempt by remember(validated) { mutableIntStateOf(0) }
    val activeUrl = candidates.getOrElse(attempt) { candidates.last() }

    val px = remember(density) {
        with(density) { 52.dp.roundToPx().coerceAtLeast(1) }
    }

    val request = remember(activeUrl, px) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .size(Size(px, px))
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = request)
    val coin = painterResource(R.drawable.ic_coin)

    LaunchedEffect(painter.state, attempt, activeUrl, achievementId, candidates.size) {
        val s = painter.state
        if (s is AsyncImagePainter.State.Error) {
            if (attempt < candidates.lastIndex) {
                Log.w(ACH_LOG, "Coil 404/ошибка id=$achievementId url=$activeUrl — пробуем fallback")
                attempt++
            } else {
                Log.e(
                    ACH_LOG,
                    "Coil ERROR id=$achievementId url=$activeUrl cause=${s.result.throwable.message}",
                    s.result.throwable
                )
            }
        }
    }

    Box(modifier.then(Modifier.size(52.dp)), contentAlignment = Alignment.Center) {
        val st = painter.state
        when (st) {
            is AsyncImagePainter.State.Error ->
                Image(
                    painter = coin,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            else ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
        }
        if (st is AsyncImagePainter.State.Loading || st is AsyncImagePainter.State.Empty) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = IconBrandGreen,
                strokeWidth = 2.dp
            )
        }
    }
}
