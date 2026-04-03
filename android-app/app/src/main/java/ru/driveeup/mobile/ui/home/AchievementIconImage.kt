package ru.driveeup.mobile.ui.home

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
 * Иконка достижения через API `/api/achievements/icons/…`.
 * Размер декодирования берётся из переданного [modifier] (ожидается область с ненулевыми constraints).
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
        Box(modifier.fillMaxSize())
        return
    }

    val coin = painterResource(R.drawable.ic_coin)

    key(validated) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val pxW = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
            val pxH = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }

            val request = remember(validated, pxW, pxH) {
                ImageRequest.Builder(context)
                    .data(Uri.parse(validated))
                    .size(Size(pxW, pxH))
                    .allowHardware(false)
                    .crossfade(true)
                    .build()
            }

            val painter = rememberAsyncImagePainter(model = request)

            LaunchedEffect(painter.state, validated, achievementId) {
                val s = painter.state
                if (s is AsyncImagePainter.State.Error) {
                    Log.e(
                        ACH_LOG,
                        "Coil ERROR id=$achievementId url=$validated cause=${s.result.throwable.message}",
                        s.result.throwable
                    )
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.size(28.dp),
                        color = IconBrandGreen,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
