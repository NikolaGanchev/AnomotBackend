package com.anomot.anomotbackend.dto

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size
import javax.validation.constraints.NotNull
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