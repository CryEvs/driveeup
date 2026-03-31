package ru.driveeup.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.driveeup.mobile.domain.UserRole

@Composable
fun LoginScreen(vm: AuthViewModel, onLoggedIn: () -> Unit, goToRegister: () -> Unit) {
    val state by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.user) { if (state.user != null) onLoggedIn() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Вход")
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.login(email, password) }, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
        Button(onClick = goToRegister, modifier = Modifier.fillMaxWidth()) { Text("К регистрации") }
        state.error?.let { Text(it) }
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onRegistered: () -> Unit, goToLogin: () -> Unit) {
    val state by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.PASSENGER) }

    LaunchedEffect(state.user) { if (state.user != null) onRegistered() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Регистрация")
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { role = UserRole.PASSENGER }, modifier = Modifier.fillMaxWidth()) { Text("Роль: Пассажир") }
        Button(onClick = { role = UserRole.DRIVER }, modifier = Modifier.fillMaxWidth()) { Text("Роль: Водитель") }
        Button(onClick = { vm.register(email, password, role) }, modifier = Modifier.fillMaxWidth()) { Text("Создать аккаунт") }
        Button(onClick = goToLogin, modifier = Modifier.fillMaxWidth()) { Text("Ко входу") }
        state.error?.let { Text(it) }
    }
}
