package ru.driveeup.mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.driveeup.mobile.domain.UserRole

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val state by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.PASSENGER) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state.darkTheme) Color(0xFF0D1A0D) else Color(0xFFF4FBE9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = (if (state.registerMode) "Регистрация" else "Авторизация") + " DriveeUP",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (state.registerMode) {
                        Button(
                            onClick = { role = if (role == UserRole.PASSENGER) UserRole.DRIVER else UserRole.PASSENGER },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (role == UserRole.PASSENGER) "Роль: Пассажир" else "Роль: Водитель")
                        }
                    }
                    Button(
                        onClick = {
                            if (state.registerMode) vm.register(email, password, role) else vm.login(email, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading
                    ) {
                        Text(if (state.registerMode) "Создать аккаунт" else "Войти")
                    }
                    Button(
                        onClick = { vm.setRegisterMode(!state.registerMode) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading
                    ) {
                        Text(if (state.registerMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Регистрация")
                    }
                    if (state.loading) {
                        CircularProgressIndicator()
                    }
                    state.error?.let {
                        Text(text = it, color = Color(0xFFC93232))
                    }
                }
            }
        }
    }
}
