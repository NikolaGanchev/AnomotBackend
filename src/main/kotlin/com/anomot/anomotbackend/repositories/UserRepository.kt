package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}