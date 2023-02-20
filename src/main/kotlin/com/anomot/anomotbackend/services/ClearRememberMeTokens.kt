package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.RememberMeTokenRepository
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@ConditionalOnProperty(
        value = ["clear.remember-me"], havingValue = "true"
)
@Component
class ClearRememberMeTokens @Autowired constructor(
        private val rememberMeTokenRepository: RememberMeTokenRepository
) {

    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    @Transactional
    fun clearEmailVerificationCodes() {
        rememberMeTokenRepository.deleteOldTokens(Date.from(Instant.now().minusSeconds(Constants.REMEMBER_ME_VALIDITY_DURATION.toLong())))
    }
}