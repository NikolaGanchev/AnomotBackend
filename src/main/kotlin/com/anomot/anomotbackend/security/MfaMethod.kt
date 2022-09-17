package com.anomot.anomotbackend.security

enum class MfaMethod(val method: String) {
    TOTP("totp"),
    EMAIL("email")
}