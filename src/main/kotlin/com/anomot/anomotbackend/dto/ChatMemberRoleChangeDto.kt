package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ChatRoles
import com.anomot.anomotbackend.utils.Constants
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull

data class ChatMemberRoleChangeDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        val role: ChatRoles,
        @NotNull
        @NotEmpty
        val chatMember: String,
        @Size(min = 1, max = Constants.PASSWORD_MAX_SIZE, message = "Password has to be between 1 and ${Constants.PASSWORD_MAX_SIZE} characters")
        val password: String?
)