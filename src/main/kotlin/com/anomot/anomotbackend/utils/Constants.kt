package com.anomot.anomotbackend.utils

class Constants {

    companion object {
        const val USERNAME_PARAMETER = "email"
        const val PASSWORD_PARAMETER = "password"
        const val EMAIL_VERIFICATION_TOKEN_LIFETIME = 1
        const val MFA_EMAIL_CODE_LIFETIME: Long = 60 * 10 // 10 minutes
    }
}