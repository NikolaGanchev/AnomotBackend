package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.PasswordResetTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@ConditionalOnProperty(
        value = ["scaling.is.main"], havingValue = "true"
)
@Component
class ClearPasswordResetTokens @Autowired constructor(
        private val passwordResetTokenRepository: PasswordResetTokenRepository
) {

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    fun clearEmailVerificationCodes() {
        passwordResetTokenRepository.deleteOldTokens(Date.from(Instant.now()))
    }
}