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
        const val REMEMBER_ME_PARAMETER = "rememberMe"
        const val REMEMBER_ME_COOKIE_NAME = "remember-me"
        const val PROFILE_PIC_SIZE = 225
        const val MAX_DRAWING_TOLERANCE = 1
        const val MAX_HENTAI_TOLERANCE = 0.95
        const val MAX_NEUTRAL_TOLERANCE = 1
        const val MAX_SEXY_TOLERANCE = 0.90
        const val MAX_PORN_TOLERANCE = 0.87
        const val FOLLOWS_PER_PAGE = 30
        const val MAX_POST_LENGTH = 6200
        const val MIN_POST_LENGTH = 50
        const val MAX_ELO_DIFFERENCE = 800
        const val BATTLE_DURATION = 3 * 24 * 3600 // seconds
        const val MEDIA_PAGE = 9
        const val POST_PAGE = 9
        const val FEED_PAGE = 8
        const val VOTE_PAGE = 20
        const val BATTLE_PAGE = 10
        const val LIKED_BY_PAGE = 20
        const val ABSOLUTE_POST_TEXT_MAX_SIZE_LIMIT = 30_000
        const val ABSOLUTE_POST_TEXT_MIN_SIZE_LIMIT = 1
        const val MIN_URL_LENGTH = 64
        const val URL_LENGTH = 64
        const val COMMENTS_PAGE = 20
        const val MAX_COMMENT_SIZE = 2000
        const val MIN_COMMENT_SIZE = 1
    }
}