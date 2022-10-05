package com.anomot.anomotbackend.utils

class Constants {

    companion object {
        const val USERNAME_PARAMETER = "email"
        const val PASSWORD_PARAMETER = "password"
        const val MFA_CODE_PARAMETER = "mfaCode"
        const val MFA_METHOD_PARAMETER = "mfaMethod"
        const val MFA_RECOVERY_CODE_PARAMETER = "mfaRecoveryCode"
        const val EMAIL_VERIFICATION_TOKEN_LIFETIME = 1
        const val MFA_EMAIL_CODE_LIFETIME: Long = 60 * 10 // 10 minutes
        const val TOTP_PERIOD: Long = 30
        const val MFA_PASSWORD_LENGTH = 6
        const val TOTP_CODE_ALLOWED_DELAY = 2
        const val PASSWORD_MIN_SIZE = 10
        const val PASSWORD_MAX_SIZE = 512
        const val MFA_RECOVERY_CODE_LENGTH = 8
        const val MFA_RECOVERY_CODE_AMOUNT = 6
        const val EMAIL_VERIFICATION_TOKEN_LENGTH = 32
        const val PASSWORD_RESET_CODE_LENGTH = 64
        const val PASSWORD_RESET_TOKEN_LIFETIME = 60 // minutes
        const val REMEMBER_ME_VALIDITY_DURATION = 60 * 60 * 24 * 14 // two weeks
        const val REMEMBER_ME_COOKIE_DOMAIN = "domain"
        const val LOGINS_PER_PAGE = 20
    }
}