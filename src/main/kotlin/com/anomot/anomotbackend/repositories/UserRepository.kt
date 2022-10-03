package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    @Modifying
    @Query("update User user set user.isEmailVerified = ?1 where user.email = ?2")
    fun setIsEmailVerifiedByEmail(isEmailVerified: Boolean, email: String): Int

    @Modifying
    @Query("update User user set user.password=?1 where user.id = ?2")
    fun setPassword(newPassword: String, id: Long): Int

    @Modifying
    @Query("update User user set user.email=?1 where user.id = ?2")
    fun setEmail(newEmail: String, id: Long): Int

    @Modifying
    @Query("update User user set user.username = ?1 where user.id = ?2")
    fun setUsername(newUsername: String, id: Long): Int

    fun existsByEmail(email: String): Boolean
}