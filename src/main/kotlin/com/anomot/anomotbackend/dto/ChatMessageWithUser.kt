package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.ChatMessage
import com.anomot.anomotbackend.entities.User

data class ChatMessageWithUser(
        val chatMessage: ChatMessage,
        val user: User?
)