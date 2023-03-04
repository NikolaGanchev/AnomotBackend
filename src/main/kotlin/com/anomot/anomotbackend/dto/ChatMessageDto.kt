package com.anomot.anomotbackend.dto

import java.util.*

data class ChatMessageDto(
        val member: ChatMemberDto,
        val message: String,
        val creationDate: Date
)
