package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.PasswordResetTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class ClearPasswordResetTokens @Autowired constructor(
        private val passwordResetTokenRepository: PasswordResetTokenRepository
) {

    @Scheduled(cron = "0 0 0 * * ?")
    fun clearEmailVerificationCodes() {
        passwordResetTokenRepository.deleteOldTokens(Date.from(Instant.now()))
    }
}