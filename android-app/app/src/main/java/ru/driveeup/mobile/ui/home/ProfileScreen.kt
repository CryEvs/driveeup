package ru.driveeup.mobile.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

private fun uriToDataUrl(context: android.content.Context, uri: Uri): String? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null
    val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val side = minOf(src.width, src.height)
    val sx = (src.width - side) / 2
    val sy = (src.height - side) / 2
    val square = Bitmap.createBitmap(src, sx, sy, side, side)
    val resized = Bitmap.createScaledBitmap(square, 320, 320, true)
    val out = java.io.ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 90, out)
    return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

@Composable
private fun ProfileAvatar(avatarUrl: String?, modifier: Modifier = Modifier) {
    val model = avatarUrl?.takeIf { it.isNotBlank() }
    when {
        model == null -> AsyncImage(model = PLACEHOLDER, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = modifier)
        model.startsWith("data:") -> {
            val bitmap = remember(model) { decodeDataUriToBitmap(model) }
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = modifier)
            } else {
                AsyncImage(model = PLACEHOLDER, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = modifier)
            }
        }
        else -> AsyncImage(model = model, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = modifier)
    }
}

@Composable
fun ProfileScreen(
    user: User,
    onChangeAvatar: (String) -> Unit,
    onSaveProfile: (firstName: String, lastName: String, email: String, city: String) -> Unit,
    onOpenMenu: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var email by remember { mutableStateOf(user.email) }
    var city by remember { mutableStateOf(user.city) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val dataUrl = uriToDataUrl(context, uri)
            withContext(Dispatchers.Main) { dataUrl?.let(onChangeAvatar) }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val resized = Bitmap.createScaledBitmap(bitmap, 320, 320, true)
            val out = java.io.ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 90, out)
            val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            withContext(Dispatchers.Main) { onChangeAvatar(dataUrl) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp).clickable(onClick = onOpenMenu),
                shape = CircleShape,
                color = Color(0xFFF0F0F0)
            ) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
            }
            Button(
                onClick = {
                    onSaveProfile(
                        firstName.trim(),
                        lastName.trim(),
                        email.trim(),
                        city.trim()
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF96EA28),
                    contentColor = Color(0xFF1D2A08)
                )
            ) { Text("Сохранить") }
        }
        Spacer(Modifier.height(10.dp))
        Divider(color = Color(0xFFD8D8D8))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            ProfileAvatar(
                avatarUrl = user.avatarUrl,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF96EA28), CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Сделать снимок",
                    color = Color(0xFF2F6CC0),
                    modifier = Modifier.clickable { takePhoto.launch(null) }
                )
                Text(
                    "Выбрать из галереи",
                    color = Color(0xFF2F6CC0),
                    modifier = Modifier.clickable { pickImage.launch("image/*") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black)
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth().padding(start = 8.dp), singleLine = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black)
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Фамилия") }, modifier = Modifier.fillMaxWidth().padding(start = 8.dp), singleLine = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(start = 8.dp), singleLine = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null, tint = Color.Black)
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Город") }, modifier = Modifier.fillMaxWidth().padding(start = 8.dp), singleLine = true)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBF2))) {
            Column(Modifier.padding(12.dp)) {
                Text("DriveCoin: ${user.driveCoin}")
                Text("DriveCoin за все время: ${user.totalDriveCoin}")
            }
        }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Выйти")
        }
    }
}
