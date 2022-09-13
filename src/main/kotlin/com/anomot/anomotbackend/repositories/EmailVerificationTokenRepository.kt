package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.EmailVerificationToken
import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerificationTokenRepository: JpaRepository<EmailVerificationToken, Long> {
    fun findByVerificationCode(verificationCode: String): EmailVerificationToken?
}