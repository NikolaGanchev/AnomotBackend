package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.entities.RememberMeToken
import com.anomot.anomotbackend.repositories.RememberMeTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class CustomRememberMeTokenRepository @Autowired constructor(
        private val rememberMeTokenRepository: RememberMeTokenRepository
): PersistentTokenRepository {
    override fun createNewToken(token: PersistentRememberMeToken) {
        val rememberMeToken = RememberMeToken(token.series, token.tokenValue, token.username, token.date)

        rememberMeTokenRepository.save(rememberMeToken)
    }

    override fun updateToken(series: String, tokenValue: String, lastUsed: Date) {
        rememberMeTokenRepository.updateBySeries(series, tokenValue, lastUsed)
    }

    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? {
        val rememberMeToken = rememberMeTokenRepository.findBySeries(seriesId) ?: return null
        return PersistentRememberMeToken(rememberMeToken.email,
                rememberMeToken.series,
                rememberMeToken.tokenValue,
                rememberMeToken.date)
    }

    override fun removeUserTokens(email: String) {
        rememberMeTokenRepository.deleteAllByEmail(email)
    }
}