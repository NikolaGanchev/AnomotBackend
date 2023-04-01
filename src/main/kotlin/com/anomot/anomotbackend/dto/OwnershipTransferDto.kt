package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size
import javax.validation.constraints.NotNull

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