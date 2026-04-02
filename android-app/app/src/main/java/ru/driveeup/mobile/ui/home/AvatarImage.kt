package ru.driveeup.mobile.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private const val PLACEHOLDER = "https://placehold.co/120x120?text=%20"

private fun decodeDataUriToBitmap(dataUri: String): Bitmap? {
    if (!dataUri.startsWith("data:")) return null
    val comma = dataUri.indexOf(',')
    if (comma < 0) return null
    val payload = dataUri.substring(comma + 1)
    return runCatching {
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/** Аватар по URL или data:image; для заказов и профиля. */
@Composable
fun AvatarImage(
    avatarUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: String = PLACEHOLDER
) {
    val model = avatarUrl?.takeIf { it.isNotBlank() }
    when {
        model == null ->
            AsyncImage(
                model = placeholder,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        model.startsWith("data:") -> {
            val bitmap = remember(model) { decodeDataUriToBitmap(model) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                AsyncImage(
                    model = placeholder,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
        else ->
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
    }
}

@Composable
fun RideUserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AvatarImage(
        avatarUrl = avatarUrl,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
