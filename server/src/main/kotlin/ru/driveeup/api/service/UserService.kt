package ru.driveeup.api.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import ru.driveeup.api.dto.LoginRequest
import ru.driveeup.api.dto.RegisterRequest
import ru.driveeup.api.entity.User
import ru.driveeup.api.repository.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(request: RegisterRequest): User {
        require(request.email.isNotBlank()) { "Email is required" }
        require(request.password.length >= 6) { "Password must be at least 6 characters" }

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("User with this email already exists")
        }

        val user = User(
            email = request.email.trim().lowercase(),
            passwordHash = passwordEncoder.encode(request.password),
            role = request.role
        )
        return userRepository.save(user)
    }

    fun login(request: LoginRequest): User {
        val user = userRepository.findByEmail(request.email.trim().lowercase())
            .orElseThrow { IllegalArgumentException("Invalid credentials") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return user
    }

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email).orElseThrow { IllegalArgumentException("User not found") }
}
