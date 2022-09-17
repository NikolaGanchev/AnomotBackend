package com.anomot.anomotbackend.security

enum class MfaMethodValue(val method: String) {
    TOTP("totp"),
    EMAIL("email")
}