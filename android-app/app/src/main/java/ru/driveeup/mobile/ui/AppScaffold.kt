package ru.driveeup.mobile.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.driveeup.mobile.ui.auth.AuthScreen
import ru.driveeup.mobile.ui.auth.AuthViewModel
import ru.driveeup.mobile.ui.home.BattlePassScreen
import ru.driveeup.mobile.ui.home.GamesScreen
import ru.driveeup.mobile.ui.home.HomeScreen
import ru.driveeup.mobile.ui.home.ProfileScreen

enum class AppPage { HOME, PROFILE, GAMES, BATTLE_PASS }

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
                .background(Color.White)
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

    val drawerBg = MaterialTheme.colorScheme.surfaceVariant

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = drawerBg) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DriveeUP", style = MaterialTheme.typography.titleLarge)
                        Text("DriveeCoin: ${state.user?.driveeCoin ?: 0}")
                        val navColors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationDrawerItem(
                            label = { Text("Главная") },
                            selected = page == AppPage.HOME,
                            onClick = {
                                page = AppPage.HOME
                                scope.launch { drawerState.close() }
                            },
                            colors = navColors
                        )
                        NavigationDrawerItem(
                            label = { Text("Профиль") },
                            selected = page == AppPage.PROFILE,
                            onClick = {
                                page = AppPage.PROFILE
                                scope.launch { drawerState.close() }
                            },
                            colors = navColors
                        )
                        NavigationDrawerItem(
                            label = { Text("Игры") },
                            selected = page == AppPage.GAMES,
                            onClick = {
                                page = AppPage.GAMES
                                scope.launch { drawerState.close() }
                            },
                            colors = navColors
                        )
                        NavigationDrawerItem(
                            label = { Text("Батл пас") },
                            selected = page == AppPage.BATTLE_PASS,
                            onClick = {
                                page = AppPage.BATTLE_PASS
                                scope.launch { drawerState.close() }
                            },
                            colors = navColors
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeDropdown(
                            isDark = state.darkTheme,
                            onSelect = onToggleTheme
                        )
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD9534F),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Выйти")
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("DriveeUP") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (page) {
                    AppPage.HOME -> HomeScreen()
                    AppPage.PROFILE -> ProfileScreen(user = state.user!!, onChangeAvatar = onChangeAvatar)
                    AppPage.GAMES -> GamesScreen()
                    AppPage.BATTLE_PASS -> BattlePassScreen()
                }
            }
        }
    }
}

@Composable
private fun ThemeDropdown(isDark: Boolean, onSelect: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (isDark) "Тёмная" else "Светлая"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Тема", style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Светлая") },
                    onClick = {
                        onSelect(false)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Тёмная") },
                    onClick = {
                        onSelect(true)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun driveeupColorScheme(isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = Color(0xFF96EA28),
            onPrimary = Color(0xFF1D2A08),
            primaryContainer = Color(0xFF2D4A22),
            onPrimaryContainer = Color(0xFFE7F8D7),
            secondary = Color(0xFF74C515),
            onSecondary = Color(0xFF1D2A08),
            secondaryContainer = Color(0xFF3C652D),
            onSecondaryContainer = Color(0xFFE7F8D7),
            tertiary = Color(0xFF96EA28),
            onTertiary = Color(0xFF1D2A08),
            tertiaryContainer = Color(0xFF2D4A22),
            onTertiaryContainer = Color(0xFFE7F8D7),
            background = Color.White,
            onBackground = Color(0xFF1D2A08),
            surface = Color.White,
            onSurface = Color(0xFF1D2A08),
            surfaceVariant = Color(0xFF162514),
            onSurfaceVariant = Color(0xFFC8E6B0),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            outline = Color(0xFF4F7A3F),
            outlineVariant = Color(0xFF3C652D),
            surfaceTint = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF96EA28),
            onPrimary = Color(0xFF1D2A08),
            primaryContainer = Color(0xFFE4F7CB),
            onPrimaryContainer = Color(0xFF20310C),
            secondary = Color(0xFF74C515),
            onSecondary = Color(0xFF1D2A08),
            secondaryContainer = Color(0xFFD7EFB5),
            onSecondaryContainer = Color(0xFF20310C),
            tertiary = Color(0xFF74C515),
            onTertiary = Color(0xFF1D2A08),
            tertiaryContainer = Color(0xFFE4F7CB),
            onTertiaryContainer = Color(0xFF20310C),
            background = Color.White,
            onBackground = Color(0xFF1D2A08),
            surface = Color.White,
            onSurface = Color(0xFF1D2A08),
            surfaceVariant = Color(0xFFF2FBE4),
            onSurfaceVariant = Color(0xFF20310C),
            error = Color(0xFFC93232),
            onError = Color.White,
            outline = Color(0xFFB4D98B),
            outlineVariant = Color(0xFFD7EFB5),
            surfaceTint = Color.Transparent
        )
    }
}
