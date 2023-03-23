package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull
import java.util.*

data class ChatMemberBanDto(
        @NotNull
        @NotEmpty
        val chatId: String,
        @NotNull
        @NotEmpty
        val chatMemberToBanId: String,
        @Size(min = 1, max = 2000)
        @NotNull
        val reason: String,
        val until: Date
)