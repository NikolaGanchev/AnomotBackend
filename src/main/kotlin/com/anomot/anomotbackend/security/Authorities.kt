package com.anomot.anomotbackend.security

enum class Authorities(val roleName: String) {
    USER("ROLE_USER"),
    MODERATOR("ROLE_MODERATOR"),
    ADMIN("ROLE_ADMIN")
}