package com.anomot.anomotbackend.dto

data class SelfUserDto(val email: String,
                       val username: String,
                       val isEmailVerified: Boolean,
                       val roles: List<String>,
                       val isMfaActive: Boolean,
                       val mfaMethods: List<String>? = null,
                       var avatarId: String? = null)