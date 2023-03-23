package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull

data class OwnershipTransferDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        @NotNull
        @NotEmpty
        val chatMember: String,
        @Size(min = 1, max = Constants.PASSWORD_MAX_SIZE, message = "Password has to be between 1 and ${Constants.PASSWORD_MAX_SIZE} characters")
        val chatPassword: String?,
        val accountPassword: String
)