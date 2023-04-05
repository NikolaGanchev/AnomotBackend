package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ChatEventType

data class ChatEventSystemMessage(
        val chatEventType: ChatEventType,
        val memberId: String?
)
