package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword

data class AdminPasswordChangeDto(
        @ValidPassword
        val newPassword: String
)