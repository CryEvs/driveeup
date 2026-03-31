package ru.driveeup.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.driveeup.mobile.domain.User
import ru.driveeup.mobile.ui.home.BattlePassScreen
import ru.driveeup.mobile.ui.home.HomeScreen
import ru.driveeup.mobile.ui.home.ProfileScreen

enum class AppPage { HOME, PROFILE, BATTLE_PASS }

enum class AppThemeMode { LIGHT, DARK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveeUpAppScaffold(user: User) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(AppPage.HOME) }
    var theme by remember { mutableStateOf(AppThemeMode.LIGHT) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DriveeUP", style = MaterialTheme.typography.titleLarge)
                    Text("DriveeCoin: ${user.driveeCoin}")
                    NavigationDrawerItem(label = { Text("Профиль") }, selected = page == AppPage.PROFILE, onClick = {
                        page = AppPage.PROFILE
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("Батл пас") }, selected = page == AppPage.BATTLE_PASS, onClick = {
                        page = AppPage.BATTLE_PASS
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("Главная") }, selected = page == AppPage.HOME, onClick = {
                        page = AppPage.HOME
                        scope.launch { drawerState.close() }
                    })
                    Text("Тема")
                    Button(onClick = { theme = AppThemeMode.LIGHT }) { Text("Светлая") }
                    Button(onClick = { theme = AppThemeMode.DARK }) { Text("Темная") }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DriveeUP") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "menu")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    AppPage.HOME -> HomeScreen()
                    AppPage.PROFILE -> ProfileScreen(user = user, onChangeAvatar = {})
                    AppPage.BATTLE_PASS -> BattlePassScreen()
                }
            }
        }
    }
}
