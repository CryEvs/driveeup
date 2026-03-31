package ru.driveeup.mobile.data

import ru.driveeup.mobile.domain.User
import ru.driveeup.mobile.domain.UserRole

class AuthRepository {
    suspend fun login(email: String, password: String): User {
        // TODO: integrate /api/auth/login
        return User(1, email, UserRole.PASSENGER, 125, false, null)
    }

    suspend fun register(email: String, password: String, role: UserRole): User {
        // TODO: integrate /api/auth/register
        return User(2, email, role, 0, false, null)
    }

    suspend fun updateAvatar(user: User, avatarUrl: String): User {
        // TODO: integrate /api/auth/avatar
        return user.copy(avatarUrl = avatarUrl)
    }
}
