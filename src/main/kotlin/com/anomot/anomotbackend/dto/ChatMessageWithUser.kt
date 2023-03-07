package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.ChatMessage
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.utils.ChatRoles

data class ChatMessageWithUser(
        val chatMessage: ChatMessage,
        val roles: List<ChatRoles>,
        val user: User?
)