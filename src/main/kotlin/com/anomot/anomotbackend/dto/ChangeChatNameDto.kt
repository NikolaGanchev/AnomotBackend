package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size
import javax.validation.constraints.NotNull

data class ChangeChatNameDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username has to be between 1 and ${Constants.USERNAME_MAX_LENGTH} characters")
        val username: String,
)
