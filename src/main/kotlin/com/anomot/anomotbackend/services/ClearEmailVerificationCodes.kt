package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.EmailVerificationTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class ClearEmailVerificationCodes @Autowired constructor(
        val emailVerificationTokenRepository: EmailVerificationTokenRepository
) {

    @Scheduled(cron = "0 0 0 * * ?")
    fun clearEmailVerificationCodes() {
        emailVerificationTokenRepository.deleteOldTokens(Date.from(Instant.now()))
    }
}