package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.driveeup.mobile.domain.User

@Composable
fun DriveUpScreen(
    user: User,
    onOpenMenu: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenBattlePass: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp).clickable(onClick = onOpenMenu),
                shape = CircleShape,
                color = Color(0xFFF0F0F0)
            ) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
            }
            Text("DriveUP", modifier = Modifier.padding(start = 12.dp))
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBF2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Баланс DriveCoin")
                Text("${user.driveCoin}", color = Color(0xFF5BAA1D))
            }
        }
        Button(onClick = onOpenGames, modifier = Modifier.fillMaxWidth()) { Text("Игры") }
        Button(onClick = onOpenBattlePass, modifier = Modifier.fillMaxWidth()) { Text("Батл-Пасс") }
    }
}
