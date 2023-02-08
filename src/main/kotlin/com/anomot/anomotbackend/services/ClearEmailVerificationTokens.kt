package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.EmailVerificationTokenRepository
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
class ClearEmailVerificationTokens @Autowired constructor(
        val emailVerificationTokenRepository: EmailVerificationTokenRepository
) {

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    fun clearEmailVerificationCodes() {
        emailVerificationTokenRepository.deleteOldTokens(Date.from(Instant.now()))
    }
}