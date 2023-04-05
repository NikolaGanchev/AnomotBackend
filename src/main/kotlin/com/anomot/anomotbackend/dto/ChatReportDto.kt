package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ChatReportReason
import javax.validation.constraints.Size

data class ChatReportDto(
        val reason: ChatReportReason,
        val chatId: String,
        @Size(min = 0, max = 1000)
        val other: String?
)