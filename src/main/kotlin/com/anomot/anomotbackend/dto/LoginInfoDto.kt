package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.SuccessfulLogin
import java.time.Instant
import java.util.*

data class LoginInfoDto(val city: String,
                        val country: String,
                        val deviceType: String,
                        val platform: String,
                        val platformVersion: String,
                        val browser: String,
                        val browserVersion: String,
                        val date: Date) {
    companion object {
        fun from(successfulLogin: SuccessfulLogin): LoginInfoDto {
            return LoginInfoDto(
                    getString(successfulLogin.city),
                    getString(successfulLogin.country!!),
                    getString(successfulLogin.deviceType),
                    getString(successfulLogin.platform),
                    getString(successfulLogin.platformVersion),
                    getString(successfulLogin.browser),
                    getString(successfulLogin.browserVersion),
                    if (successfulLogin.date != null) successfulLogin.date!! else Date.from(Instant.EPOCH)
            )
        }

        private fun getString(string: String?): String {
            return string ?: "Unknown"
        }
    }
}