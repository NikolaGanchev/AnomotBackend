package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword

data class PasswordChangeDto(
        @ValidPassword
        val oldPassword: String,
        @ValidPassword
        val newPassword: String
)