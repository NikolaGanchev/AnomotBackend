package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class ChatMessageSendDto(
    @Max(Constants.MAX_CHAT_MESSAGE_SIZE.toLong())
    @Min(Constants.MIN_CHAT_MESSAGE_SIZE.toLong())
    val text: String
)