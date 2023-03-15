package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import org.jetbrains.annotations.NotNull
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class ChatJoinDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username has to be between 1 and ${Constants.USERNAME_MAX_LENGTH} characters")
        val username: String,
        val password: String?
)
