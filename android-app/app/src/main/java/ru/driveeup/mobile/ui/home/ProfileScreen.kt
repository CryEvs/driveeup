package ru.driveeup.mobile.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.driveeup.mobile.domain.User

private const val PLACEHOLDER = "https://placehold.co/120x120?text=Avatar"

private fun decodeDataUriToBitmap(dataUri: String): android.graphics.Bitmap? {
    if (!dataUri.startsWith("data:")) return null
    val comma = dataUri.indexOf(',')
    if (comma < 0) return null
    val meta = dataUri.substring(5, comma)
    if (!meta.contains("base64", ignoreCase = true)) return null
    val payload = dataUri.substring(comma + 1)
    return runCatching {
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun uriToDataUrl(context: android.content.Context, uri: Uri): String? {
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$b64"
}

@Composable
private fun ProfileAvatar(avatarUrl: String?, modifier: Modifier = Modifier) {
    val model = avatarUrl?.takeIf { it.isNotBlank() }

    when {
        model == null -> {
            AsyncImage(
                model = PLACEHOLDER,
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        model.startsWith("data:") -> {
            val bitmap = remember(model) { decodeDataUriToBitmap(model) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                AsyncImage(
                    model = PLACEHOLDER,
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
        else -> {
            AsyncImage(
                model = model,
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
}

@Composable
fun ProfileScreen(user: User, onChangeAvatar: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val dataUrl = uriToDataUrl(context, uri)
            withContext(Dispatchers.Main) {
                dataUrl?.let { onChangeAvatar(it) }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                val avatarModifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF96EA28), CircleShape)

                ProfileAvatar(
                    avatarUrl = user.avatarUrl,
                    modifier = avatarModifier
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Email: ${user.email}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Роль: ${user.role}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { pickImage.launch("image/*") },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Изменить аватарку")
                    }
                }
            }
        }
    }
}
