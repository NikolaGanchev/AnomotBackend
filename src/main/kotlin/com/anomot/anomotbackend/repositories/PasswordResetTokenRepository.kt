package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.PasswordResetToken
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PasswordResetTokenRepository: JpaRepository<PasswordResetToken, Long> {
    fun findByIdentifier(identifier: String): PasswordResetToken?

    @Modifying
    @Query("delete from PasswordResetToken token where token.expiryDate < ?1")
    fun deleteOldTokens(now: Date): Int

    @Modifying
    @Query("delete from PasswordResetToken token where token.user = ?1")
    fun deleteByUser(user: User)
}