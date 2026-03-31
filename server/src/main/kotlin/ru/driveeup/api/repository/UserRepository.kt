package ru.driveeup.api.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.driveeup.api.entity.User
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}
