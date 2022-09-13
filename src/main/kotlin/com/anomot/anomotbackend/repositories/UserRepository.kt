package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    @Modifying
    @Query("update User user set user.isEmailVerified = ?1 where user.email = ?2")
    fun setIsEmailVerifiedByEmail(isEmailVerified: Boolean, email: String): Int
}