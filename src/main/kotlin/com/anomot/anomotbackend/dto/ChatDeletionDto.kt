package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ChatDeletionDto(
        @NotEmpty
        @NotNull
        val chatId: String,
        @Size(min = 1, max = Constants.PASSWORD_MAX_SIZE, message = "Password has to be between 1 and ${Constants.PASSWORD_MAX_SIZE} characters")
        val password: String?,
)
