package com.anomot.anomotbackend.utils

class Constants {

    companion object {
        const val USERNAME_PARAMETER = "email"
        const val PASSWORD_PARAMETER = "password"
        const val MFA_CODE_PARAMETER = "mfaCode"
        const val MFA_METHOD_PARAMETER = "mfaMethod"
        const val MFA_AVAILABLE_METHODS_PARAMETER = "mfaMethods"
        const val MFA_SHOULD_SEND_MFA_EMAIL = "shouldSendMfaEmail"
        const val EMAIL_VERIFICATION_TOKEN_LIFETIME = 1
        const val MFA_EMAIL_CODE_LIFETIME: Long = 60 * 10 // 10 minutes
        const val TOTP_PERIOD: Long = 30
        const val TOTP_PASSWORD_LENGTH = 6
        const val TOTP_CODE_ALLOWED_DELAY = 2
    }
}