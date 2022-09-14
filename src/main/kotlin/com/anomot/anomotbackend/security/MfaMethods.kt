package com.anomot.anomotbackend.security

enum class MfaMethods(val method: String) {
    TOTP("TOTP"),
    EMAIL("email")
}