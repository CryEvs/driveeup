package ru.driveeup.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val state by vm.uiState.collectAsState()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (state.registerMode) "Регистрация" else "Вход",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (state.registerMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("Имя") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Фамилия") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
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
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Подтверждение пароля") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            if (state.registerMode) {
                                vm.register(firstName, lastName, email, password, confirmPassword)
                            } else {
                                vm.login(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (state.registerMode) "Зарегистрироваться" else "Войти")
                    }
                    TextButton(
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
