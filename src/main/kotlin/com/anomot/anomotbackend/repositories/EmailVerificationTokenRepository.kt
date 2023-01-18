package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.EmailVerificationToken
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface EmailVerificationTokenRepository: JpaRepository<EmailVerificationToken, Long> {
    fun findByVerificationCode(verificationCode: String): EmailVerificationToken?

    @Modifying
    @Query("delete from EmailVerificationToken token where token.expiryDate < ?1")
    fun deleteOldTokens(now: Date): Int

    @Modifying
    @Query("delete from EmailVerificationToken token where token.user = ?1")
    fun deleteByUser(user: User)
}