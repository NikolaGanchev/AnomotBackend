package com.anomot.anomotbackend.dto

import java.util.*

data class ChatMessageDto(
        val member: ChatMemberDto,
        val message: String,
        val isSystem: Boolean,
        val creationDate: Date
)
