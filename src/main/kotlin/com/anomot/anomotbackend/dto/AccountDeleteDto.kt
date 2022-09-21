package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword

data class AccountDeleteDto(
        @ValidPassword
        val password: String
)