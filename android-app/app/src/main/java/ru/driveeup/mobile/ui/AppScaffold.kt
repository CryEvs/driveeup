package ru.driveeup.mobile.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.driveeup.mobile.ui.auth.AuthScreen
import ru.driveeup.mobile.ui.auth.AuthViewModel
import ru.driveeup.mobile.ui.home.BattlePassScreen
import ru.driveeup.mobile.ui.home.HomeScreen
import ru.driveeup.mobile.ui.home.ProfileScreen

enum class AppPage { HOME, PROFILE, BATTLE_PASS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveeUpAppScaffold() {
    val vm = remember { AuthViewModel() }
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("driveeup_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        vm.restoreSession(
            token = prefs.getString("token", "").orEmpty(),
            darkTheme = prefs.getBoolean("darkTheme", false)
        )
    }
    LaunchedEffect(state.token) {
        prefs.edit().putString("token", state.token).apply()
    }
    LaunchedEffect(state.darkTheme) {
        prefs.edit().putBoolean("darkTheme", state.darkTheme).apply()
    }

    val colorScheme = remember(state.darkTheme) { driveeupColorScheme(state.darkTheme) }
    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.darkTheme) Color(0xFF0D1A0D) else Color(0xFFF4FBE9))
        ) {
            if (state.token.isBlank() || state.user == null) {
                AuthScreen(vm = vm)
            } else {
                AppContent(
                    state = state,
                    onToggleTheme = { vm.setTheme(it) },
                    onLogout = { vm.logout() },
                    onChangeAvatar = { vm.updateAvatar(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
    state: ru.driveeup.mobile.ui.auth.AuthUiState,
    onToggleTheme: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onChangeAvatar: (String) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(AppPage.HOME) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DriveeUP", style = MaterialTheme.typography.titleLarge)
                    Text("DriveeCoin: ${state.user?.driveeCoin ?: 0}")
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
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Тема")
                        Button(onClick = { onToggleTheme(false) }, modifier = Modifier.fillMaxWidth()) { Text("Светлая") }
                        Button(onClick = { onToggleTheme(true) }, modifier = Modifier.fillMaxWidth()) { Text("Тёмная") }
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9534F), contentColor = Color.White)
                        ) {
                            Text("Выйти")
                        }
                    }
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
                    AppPage.PROFILE -> ProfileScreen(user = state.user!!, onChangeAvatar = onChangeAvatar)
                    AppPage.BATTLE_PASS -> BattlePassScreen()
                }
            }
        }
    }
}

private fun driveeupColorScheme(isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = Color(0xFF96EA28),
            onPrimary = Color(0xFF1D2A08),
            background = Color(0xFF0D1A0D),
            onBackground = Color(0xFFE7F8D7),
            surface = Color(0xFF162514),
            onSurface = Color(0xFFE7F8D7)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF96EA28),
            onPrimary = Color(0xFF1D2A08),
            background = Color(0xFFF4FBE9),
            onBackground = Color(0xFF1D2A08),
            surface = Color.White,
            onSurface = Color(0xFF1D2A08)
        )
    }
}
