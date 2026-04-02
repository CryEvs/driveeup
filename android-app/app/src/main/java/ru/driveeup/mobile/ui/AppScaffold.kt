package ru.driveeup.mobile.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.driveeup.mobile.ui.auth.AuthScreen
import ru.driveeup.mobile.ui.auth.AuthViewModel
import ru.driveeup.mobile.ui.home.BattlePassScreen
import ru.driveeup.mobile.domain.UserRole
import ru.driveeup.mobile.ui.home.CityScreen
import ru.driveeup.mobile.ui.home.DriverCityScreen
import ru.driveeup.mobile.ui.home.DriveUpScreen
import ru.driveeup.mobile.ui.home.DriveUpLoyaltyLevelsScreen
import ru.driveeup.mobile.ui.home.DriveUpNotificationsScreen
import ru.driveeup.mobile.ui.home.DriveUpStoreAllScreen
import ru.driveeup.mobile.ui.home.DriveUpTasksAllScreen
import ru.driveeup.mobile.ui.home.GamesScreen
import ru.driveeup.mobile.ui.home.ProfileScreen

enum class AppPage {
    CITY, HISTORY, INTERCITY, SECURITY, SETTINGS, HELP, SUPPORT, DRIVE_UP, DRIVE_UP_LEVELS, DRIVE_UP_STORE_ALL, DRIVE_UP_TASKS_ALL, DRIVE_UP_NOTIFICATIONS, PROFILE, GAMES, BATTLE_PASS
}

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
    val typography = remember { driveeupTypography() }
    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                state.sessionChecking -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF96EA28))
                    }
                }
                state.token.isBlank() || state.user == null -> {
                    AuthScreen(vm = vm)
                }
                else -> {
                    AppContent(
                        vm = vm,
                        state = state,
                        onToggleTheme = { vm.setTheme(it) },
                        onLogout = { vm.logout() },
                        onChangeAvatar = { vm.updateAvatar(it) },
                        onSaveProfile = { firstName, lastName, email, city ->
                            vm.updateProfile(firstName, lastName, email, city)
                        },
                        onConsumeProfileSaved = { vm.consumeProfileSaved() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
    vm: AuthViewModel,
    state: ru.driveeup.mobile.ui.auth.AuthUiState,
    onToggleTheme: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onChangeAvatar: (String) -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onConsumeProfileSaved: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(AppPage.CITY) }
    val drawerBg = Color.White
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.profileSaved) {
        if (state.profileSaved) {
            page = AppPage.CITY
            snackbarHostState.showSnackbar("Изменения успешно применены")
            scope.launch { drawerState.open() }
            onConsumeProfileSaved()
        }
    }

    LaunchedEffect(state.token) {
        val t = state.token
        if (t.isBlank()) return@LaunchedEffect
        while (true) {
            delay(8000)
            vm.refreshMe(t)
        }
    }

    /** Интерфейс водителя / пассажира берём из роли аккаунта на сервере, а не из локального prefs. */
    val isDriverUi = state.user?.role == UserRole.DRIVER

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    page = AppPage.PROFILE
                                    scope.launch { drawerState.close() }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFFEAEAEA), modifier = Modifier.size(42.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = listOfNotNull(state.user?.firstName, state.user?.lastName)
                                        .joinToString(" ")
                                        .ifBlank { state.user?.email ?: "Профиль" }
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("★", color = Color(0xFFFFC107))
                                    Text(
                                        " ${state.user?.ratingAvg ?: 5.0} (${state.user?.ridesCount ?: 0})",
                                        color = Color(0xFF6C6C6C),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Text(">", color = Color.Gray)
                        }
                        Divider(color = Color(0xFFD7D7D7))
                        MenuItem(page, AppPage.CITY, "Город", Icons.Default.Place) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.HISTORY, "История заказов", Icons.Default.Info) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.INTERCITY, "Межгород", Icons.Default.Place) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.SECURITY, "Безопасность", Icons.Default.Info) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.SETTINGS, "Настройки", Icons.Default.Settings) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.HELP, "Помощь", Icons.Default.Info) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.SUPPORT, "Служба поддержки", Icons.Default.Info) { page = it; scope.launch { drawerState.close() } }
                        MenuItem(page, AppPage.DRIVE_UP, "DriveUP", Icons.Default.Info) { page = it; scope.launch { drawerState.close() } }
                    }
                    Column {
                        Divider(color = Color(0xFFD7D7D7))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val next = if (isDriverUi) UserRole.PASSENGER else UserRole.DRIVER
                                vm.setRole(next)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF96EA28),
                                contentColor = Color.White
                            )
                        ) {
                            Text(if (isDriverUi) "Стать пассажиром" else "Стать водителем")
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = Color.White
            ) {
                when (page) {
                    AppPage.CITY -> {
                        if (isDriverUi) {
                            DriverCityScreen(
                                driveCoin = state.user?.driveCoin ?: 0.0,
                                token = state.token,
                                user = state.user!!,
                                onOpenMenu = { scope.launch { drawerState.open() } },
                                onOpenDriveUp = { page = AppPage.DRIVE_UP }
                            )
                        } else {
                            CityScreen(
                                driveCoin = state.user?.driveCoin ?: 0.0,
                                token = state.token,
                                user = state.user!!,
                                onOpenMenu = { scope.launch { drawerState.open() } },
                                onOpenDriveUp = { page = AppPage.DRIVE_UP },
                                onOpenGames = { page = AppPage.GAMES }
                            )
                        }
                    }
                    AppPage.PROFILE -> ProfileScreen(
                        user = state.user!!,
                        onChangeAvatar = onChangeAvatar,
                        onSaveProfile = onSaveProfile,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onLogout = onLogout
                    )
                    AppPage.DRIVE_UP -> DriveUpScreen(
                        user = state.user!!,
                        token = state.token,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenGames = { page = AppPage.GAMES },
                        onOpenBattlePass = { page = AppPage.BATTLE_PASS },
                        onOpenStoreAll = { page = AppPage.DRIVE_UP_STORE_ALL },
                        onOpenTasksAll = { page = AppPage.DRIVE_UP_TASKS_ALL },
                        onOpenLoyaltyLevels = { page = AppPage.DRIVE_UP_LEVELS },
                        onNotifications = { page = AppPage.DRIVE_UP_NOTIFICATIONS }
                    )
                    AppPage.DRIVE_UP_LEVELS -> DriveUpLoyaltyLevelsScreen(
                        user = state.user!!,
                        token = state.token,
                        onBack = { page = AppPage.DRIVE_UP },
                        onMenuBack = { scope.launch { drawerState.open() } },
                        onNotifications = { page = AppPage.DRIVE_UP_NOTIFICATIONS }
                    )
                    AppPage.DRIVE_UP_STORE_ALL -> DriveUpStoreAllScreen(
                        user = state.user!!,
                        token = state.token,
                        onBack = { page = AppPage.DRIVE_UP },
                        onMenuBack = { scope.launch { drawerState.open() } },
                        onNotifications = { page = AppPage.DRIVE_UP_NOTIFICATIONS }
                    )
                    AppPage.DRIVE_UP_TASKS_ALL -> DriveUpTasksAllScreen(
                        user = state.user!!,
                        token = state.token,
                        onBack = { page = AppPage.DRIVE_UP },
                        onMenuBack = { scope.launch { drawerState.open() } },
                        onNotifications = { page = AppPage.DRIVE_UP_NOTIFICATIONS }
                    )
                    AppPage.DRIVE_UP_NOTIFICATIONS -> DriveUpNotificationsScreen(
                        token = state.token,
                        onBack = { page = AppPage.DRIVE_UP }
                    )
                    AppPage.GAMES -> GamesScreen(
                        token = state.token,
                        userRole = state.user?.role ?: UserRole.PASSENGER,
                        onBack = { page = AppPage.DRIVE_UP },
                        onNavigateToCity = { page = AppPage.CITY }
                    )
                    AppPage.BATTLE_PASS -> BattlePassScreen(
                        token = state.token,
                        onBack = { page = AppPage.DRIVE_UP }
                    )
                    AppPage.HISTORY -> PlaceholderScreen("История заказов", onOpenMenu = { scope.launch { drawerState.open() } })
                    AppPage.INTERCITY -> PlaceholderScreen("Межгород", onOpenMenu = { scope.launch { drawerState.open() } })
                    AppPage.SECURITY -> PlaceholderScreen("Безопасность", onOpenMenu = { scope.launch { drawerState.open() } })
                    AppPage.SETTINGS -> PlaceholderScreen("Настройки", onOpenMenu = { scope.launch { drawerState.open() } })
                    AppPage.HELP -> PlaceholderScreen("Помощь", onOpenMenu = { scope.launch { drawerState.open() } })
                    AppPage.SUPPORT -> PlaceholderScreen("Служба поддержки", onOpenMenu = { scope.launch { drawerState.open() } })
                }
            }
        }
    }
}

@Composable
private fun MenuItem(current: AppPage, target: AppPage, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (AppPage) -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        label = { Text(label) },
        selected = current == target,
        onClick = { onClick(target) },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFFF2F2F2),
            selectedTextColor = Color(0xFF1D2A08),
            unselectedTextColor = Color(0xFF4E4E4E)
        )
    )
}

@Composable
private fun PlaceholderScreen(title: String, onOpenMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = Color(0xFFF0F0F0)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = Color(0xFF6C6C6C),
                modifier = Modifier
                    .padding(10.dp)
                    .clickable(onClick = onOpenMenu)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Раздел в разработке.")
    }
}

/** На Android [FontFamily.SansSerif] соответствует Roboto. */
private fun driveeupTypography(): Typography {
    val f = FontFamily.SansSerif
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = f),
        displayMedium = base.displayMedium.copy(fontFamily = f),
        displaySmall = base.displaySmall.copy(fontFamily = f),
        headlineLarge = base.headlineLarge.copy(fontFamily = f),
        headlineMedium = base.headlineMedium.copy(fontFamily = f),
        headlineSmall = base.headlineSmall.copy(fontFamily = f),
        titleLarge = base.titleLarge.copy(fontFamily = f),
        titleMedium = base.titleMedium.copy(fontFamily = f),
        titleSmall = base.titleSmall.copy(fontFamily = f),
        bodyLarge = base.bodyLarge.copy(fontFamily = f),
        bodyMedium = base.bodyMedium.copy(fontFamily = f),
        bodySmall = base.bodySmall.copy(fontFamily = f),
        labelLarge = base.labelLarge.copy(fontFamily = f),
        labelMedium = base.labelMedium.copy(fontFamily = f),
        labelSmall = base.labelSmall.copy(fontFamily = f)
    )
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
