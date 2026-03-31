package ru.driveeup.api.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import ru.driveeup.api.dto.*
import ru.driveeup.api.entity.User
import ru.driveeup.api.security.JwtService
import ru.driveeup.api.service.UserService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtService: JwtService
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = userService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): AuthResponse {
        val user = userService.login(request)
        return authResponse(user)
    }

    @GetMapping("/me")
    fun me(authentication: Authentication): UserResponse {
        val user = userService.findByEmail(authentication.name)
        return user.toResponse()
    }

    private fun authResponse(user: User): AuthResponse = AuthResponse(
        accessToken = jwtService.generateToken(user.email),
        user = user.toResponse()
    )
}

private fun User.toResponse() = UserResponse(
    id = id ?: 0,
    email = email,
    role = role,
    driveeCoin = driveeCoin,
    premium = premium
)
