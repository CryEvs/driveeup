package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.driveeup.mobile.domain.User

@Composable
fun ProfileScreen(user: User, onChangeAvatar: (String) -> Unit) {
    var avatarUrlInput by remember { mutableStateOf(user.avatarUrl.orEmpty()) }

    LaunchedEffect(user.avatarUrl) {
        avatarUrlInput = user.avatarUrl.orEmpty()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Профиль")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncImage(
                    model = user.avatarUrl ?: "https://placehold.co/120x120?text=Avatar",
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(CircleShape)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Email: ${user.email}")
                    Text("Роль: ${user.role}")
                    Text("DriveeCoin: ${user.driveeCoin}")
                }
            }
            OutlinedTextField(
                value = avatarUrlInput,
                onValueChange = { avatarUrlInput = it },
                label = { Text("Ссылка на аватар") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = { onChangeAvatar(avatarUrlInput) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сохранить аватар")
            }
        }
    }
}
