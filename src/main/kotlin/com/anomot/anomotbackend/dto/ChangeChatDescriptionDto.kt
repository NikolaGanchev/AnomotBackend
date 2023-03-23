package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull

data class ChangeChatDescriptionDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        @Size(min = 1, max = Constants.MAX_POST_LENGTH, message = "Description has to be between 1 and ${Constants.MAX_POST_LENGTH} characters")
        val description: String?,
        @Size(min = 1, max = Constants.PASSWORD_MAX_SIZE, message = "Password has to be between 1 and ${Constants.PASSWORD_MAX_SIZE} characters")
        val password: String?,
)
