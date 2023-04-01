package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ChatRoles
import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

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